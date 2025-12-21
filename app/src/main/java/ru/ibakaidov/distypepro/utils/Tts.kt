package ru.ibakaidov.distypepro.utils

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.annotation.VisibleForTesting
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import ru.ibakaidov.distypepro.R

class Tts(
    context: Context,
    private val locale: Locale = Locale.getDefault()
) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val cacheManager = TtsCacheManager(appContext)
    private val okHttpClient = OkHttpClient()
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventsFlow = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 16)

    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var lastAudioFile: File? = null
    private var onInitCallback: Callback<Int>? = null
    private var onPlayCallback: Callback<ProgressState>? = null
    private var useYandexCache: Boolean = getUseYandex()
    private var currentVoiceId: String =
        prefs.getString(PREF_VOICE_URI, DEFAULT_YANDEX_VOICE) ?: DEFAULT_YANDEX_VOICE
    private var lastErrorMessage: String? = null

    private var ttsReady = CompletableDeferred<Boolean>()
    private var cachedYandexVoices: List<YandexVoice>? = null
    private var voicesLoadingInProgress = false

    init {
        initializeTextToSpeech()
        loadYandexVoicesAsync()
    }

    private fun initializeTextToSpeech(force: Boolean = false) {
        if (textToSpeech != null && !force) return

        if (force) {
            textToSpeech?.shutdown()
            textToSpeech = null
        }

        if (ttsReady.isCompleted) {
            ttsReady = CompletableDeferred()
        }

        val readySignal = ttsReady

        textToSpeech = TextToSpeech(appContext) { status ->
            onInitCallback?.onDone(status)
            if (!readySignal.isCompleted) {
                readySignal.complete(status == TextToSpeech.SUCCESS)
            }

            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = locale
                textToSpeech?.setSpeechRate(getRate())
                textToSpeech?.setPitch(getPitch())
                maybeApplyStoredOfflineVoice()
            } else {
                lastErrorMessage = "TextToSpeech init error: $status"
            }
        }.apply {
            this?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    emitPlaybackStarted()
                }

                override fun onDone(utteranceId: String?) {
                    emitPlaybackCompleted()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    emitError("Ошибка воспроизведения")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    emitError("TTS error code: $errorCode")
                }
            })
        }
    }

    fun setOnInitCallback(callback: Callback<Int>) {
        onInitCallback = callback
    }

    fun setOnPlayCallback(callback: Callback<ProgressState>) {
        onPlayCallback = callback
    }

    fun events(): SharedFlow<TtsEvent> = eventsFlow.asSharedFlow()

    fun getLastError(): String? = lastErrorMessage

    fun getUseYandex(): Boolean =
        prefs.getBoolean(PREF_USE_YANDEX, true)

    fun setUseYandex(value: Boolean) {
        useYandexCache = value
        prefs.edit().putBoolean(PREF_USE_YANDEX, value).apply()
        if (!value) {
            mainScope.launch {
                ensureTtsReady()
                maybeApplyStoredOfflineVoice()
            }
        }
    }

    fun getVolume(): Float = prefs.getFloat(PREF_VOLUME, 1.0f)

    fun setVolume(value: Float) {
        val normalized = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(PREF_VOLUME, normalized).apply()
        mediaPlayer?.setVolume(normalized, normalized)
    }

    fun getRate(): Float = prefs.getFloat(PREF_RATE, 1.0f)

    fun setRate(value: Float) {
        val normalized = value.coerceIn(0.1f, 2.0f)
        prefs.edit().putFloat(PREF_RATE, normalized).apply()
        textToSpeech?.setSpeechRate(normalized)
    }

    fun getPitch(): Float = prefs.getFloat(PREF_PITCH, 1.0f)

    fun setPitch(value: Float) {
        val normalized = value.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(PREF_PITCH, normalized).apply()
        textToSpeech?.setPitch(normalized)
    }

    fun speak(text: String, download: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (download || useYandexCache) {
            mainScope.launch {
                speakOnline(trimmed, download)
            }
        } else {
            mainScope.launch {
                speakOffline(trimmed)
            }
        }
    }

    fun stop() {
        mainScope.launch {
            stopOnlinePlayback()
            textToSpeech?.stop()
            emitPlaybackCompleted()
        }
    }

    fun playLastAudio() {
        val file = lastAudioFile
        if (file == null || !file.exists()) {
            emitError("Нет сохраненных аудиофайлов")
            return
        }
        mainScope.launch {
            playAudioFile(file)
        }
    }

    fun downloadPhrasesToCache(
        phrases: List<String>,
        voice: String?,
        onProgress: (current: Int, total: Int) -> Unit
    ) {
        ioScope.launch {
            eventsFlow.tryEmit(TtsEvent.DownloadStarted)
            val total = phrases.size
            val targetVoice = resolveYandexVoiceId(voice)
            phrases.forEachIndexed { index, phrase ->
                val cacheKey = cacheManager.generateCacheKey(phrase, targetVoice)
                if (!cacheManager.isCached(cacheKey)) {
                    val bytes = requestYandexAudio(phrase, targetVoice)
                    if (bytes != null) {
                        cacheManager.saveToCache(cacheKey, bytes)
                    }
                }
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, total)
                }
                eventsFlow.tryEmit(TtsEvent.DownloadProgress(index + 1, total))
            }
            eventsFlow.tryEmit(TtsEvent.DownloadCompleted(null))
        }
    }

    suspend fun getCacheInfo(): TtsCacheManager.TtsCacheInfo = cacheManager.getCacheInfo()

    fun setCacheEnabled(enabled: Boolean) = cacheManager.setCacheEnabled(enabled)

    fun getCacheEnabled(): Boolean = cacheManager.getCacheEnabled()

    fun setCacheSizeLimitMb(limit: Double) = cacheManager.setCacheSizeLimitMb(limit)

    fun getCacheSizeLimitMb(): Double = cacheManager.getCacheSizeLimitMb()

    suspend fun clearCache() = cacheManager.clearCache()

    private fun loadYandexVoicesAsync() {
        if (voicesLoadingInProgress) return
        voicesLoadingInProgress = true

        ioScope.launch {
            try {
                val voices = fetchVoicesFromApi()
                if (voices.isNotEmpty()) {
                    cachedYandexVoices = voices
                }
            } catch (e: Exception) {
                lastErrorMessage = "Ошибка загрузки голосов: ${e.localizedMessage}"
            } finally {
                voicesLoadingInProgress = false
            }
        }
    }

    private suspend fun fetchVoicesFromApi(): List<YandexVoice> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(VOICES_ENDPOINT)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext emptyList()
                    }

                    val body = response.body?.string() ?: return@withContext emptyList()
                    val jsonArray = JSONArray(body)
                    val voices = mutableListOf<YandexVoice>()

                    for (i in 0 until jsonArray.length()) {
                        val voiceObj = jsonArray.getJSONObject(i)
                        val roleArray = voiceObj.optJSONArray("role")
                        val roles = if (roleArray != null) {
                            List(roleArray.length()) { idx -> roleArray.getString(idx) }
                        } else {
                            null
                        }

                        voices.add(
                            YandexVoice(
                                voiceURI = voiceObj.getString("id"),
                                text = voiceObj.getString("name"),
                                langCode = voiceObj.optString("lang_code", null),
                                lang = voiceObj.optString("lang", null),
                                gender = voiceObj.optString("gender", null),
                                role = roles
                            )
                        )
                    }
                    voices
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun getYandexVoices(): List<YandexVoice> = cachedYandexVoices ?: YANDEX_VOICES

    suspend fun getOfflineVoices(): List<TtsVoice> {
        ensureTtsReady()
        val voices = textToSpeech?.voices ?: return emptyList()
        return voices.map { voice ->
            TtsVoice(
                voiceId = voice.name,





















































                title = voice.name,
                locale = voice.locale?.toLanguageTag(),
                provider = VoiceProvider.OFFLINE,
                isDefault = voice.isNetworkConnectionRequired.not()
            )
        }.sortedWith(offlineVoiceComparator())
    }

    fun getSelectedVoice(): TtsVoice {
        val yandexVoice = getYandexVoices().firstOrNull { it.voiceURI == currentVoiceId }
        if (yandexVoice != null) {
            return TtsVoice(
                voiceId = yandexVoice.voiceURI,
                title = yandexVoice.text,
                provider = VoiceProvider.YANDEX
            )
        }

        val offline = textToSpeech?.voices?.firstOrNull { it.name == currentVoiceId }
        return if (offline != null) {
            TtsVoice(
                voiceId = offline.name,
                title = offline.name,
                locale = offline.locale?.toLanguageTag(),
                provider = VoiceProvider.OFFLINE,
                isDefault = offline.isNetworkConnectionRequired.not()
            )
        } else {
            TtsVoice(
                voiceId = DEFAULT_YANDEX_VOICE,
                title = "Zahar",
                provider = VoiceProvider.YANDEX,
                isDefault = true
            )
        }
    }

    fun setVoice(targetVoiceId: String) {
        currentVoiceId = targetVoiceId
        prefs.edit().putString(PREF_VOICE_URI, targetVoiceId).apply()
        val yandexVoice = getYandexVoices().firstOrNull { it.voiceURI == targetVoiceId }
        if (yandexVoice != null) {
            return
        }
        mainScope.launch {
            ensureTtsReady()
            val target = textToSpeech?.voices?.firstOrNull { it.name == targetVoiceId }
            if (target != null) {
                applyOfflineVoice(target)
            } else {
                maybeApplyStoredOfflineVoice()
            }
        }
    }

    suspend fun awaitInitialization(): Boolean = ttsReady.await()

    fun shutdown() {
        stop()
        mainScope.cancel()
        ioScope.cancel()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private suspend fun speakOffline(text: String) {
        ensureTtsReady()
        val bundle = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume())
        }
        val speakResult = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, UUID.randomUUID().toString())
        if (speakResult == TextToSpeech.ERROR) {
            emitError("Ошибка синтеза речи")
        }
    }

    private suspend fun speakOnline(text: String, download: Boolean) {
        stopOnlinePlayback()
        val voice = resolveYandexVoiceId(currentVoiceId)
        val cacheEnabled = cacheManager.getCacheEnabled() && !download
        val cacheKey = cacheManager.generateCacheKey(text, voice)
        var audioFile: File? = null

        if (cacheEnabled) {
            audioFile = cacheManager.getCachedFile(cacheKey)
        }

        if (audioFile == null) {
            val bytes = requestYandexAudio(text, voice)
            if (bytes == null) {
                emitError("Ошибка загрузки аудио")
                return
            }

            if (download) {
                val savedPath = saveToDownloads(bytes, text)
                eventsFlow.tryEmit(TtsEvent.DownloadCompleted(savedPath))
                emitPlaybackCompleted()
                return
            }

            audioFile = if (cacheEnabled) {
                cacheManager.saveToCache(cacheKey, bytes)
            } else {
                writeTempFile(bytes)
            }
        }

        if (audioFile == null) {
            emitError("Не удалось подготовить аудио")
            return
        }

        emitPlaybackStarted()
        playAudioFile(audioFile)
    }

    private suspend fun ensureTtsReady() {
        if (textToSpeech == null) {
            initializeTextToSpeech(force = true)
        }

        val ready = ttsReady.await()
        if (!ready) {
            initializeTextToSpeech(force = true)
            ttsReady.await()
        }
    }

    private fun emitPlaybackStarted() {
        onPlayCallback?.onDone(ProgressState.START)
        eventsFlow.tryEmit(TtsEvent.SpeakingStarted)
    }

    private fun emitPlaybackCompleted() {
        onPlayCallback?.onDone(ProgressState.STOP)
        eventsFlow.tryEmit(TtsEvent.SpeakingCompleted)
    }

    private fun emitError(message: String) {
        lastErrorMessage = message
        onPlayCallback?.onDone(ProgressState.ERROR)
        eventsFlow.tryEmit(TtsEvent.Error(message))
    }

    private fun emitTemporarilyUnavailable() {
        val message = appContext.getString(R.string.tts_temporarily_unavailable)
        eventsFlow.tryEmit(TtsEvent.TemporarilyUnavailable(message))
    }

    private suspend fun requestYandexAudio(text: String, voice: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("text", text)
                    .put("voice", voice)
                    .toString()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = payload.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(TTS_ENDPOINT)
                    .post(requestBody)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code in 500..599) {
                            emitTemporarilyUnavailable()
                        }
                        lastErrorMessage = "HTTP ${response.code}"
                        eventsFlow.tryEmit(TtsEvent.Status("HTTP ${response.code}"))
                        null
                    } else {
                        response.body?.bytes()
                    }
                }
            } catch (error: IOException) {
                val message = error.localizedMessage ?: "network error"
                lastErrorMessage = message
                emitTemporarilyUnavailable()
                eventsFlow.tryEmit(TtsEvent.Status("Ошибка сети: $message"))
                null
            }
        }
    }

    private suspend fun playAudioFile(file: File) {
        withContext(Dispatchers.Main) {
            stopOnlinePlayback()
            val volume = getVolume().coerceIn(0f, 1f)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    emitPlaybackCompleted()
                    stopOnlinePlayback()
                }
                setOnErrorListener { _, what, extra ->
                    emitError("MediaPlayer error: $what / $extra")
                    stopOnlinePlayback()
                    true
                }
                setVolume(volume, volume)
                prepare()
                start()
            }
            lastAudioFile = file
        }
    }

    private fun stopOnlinePlayback() {
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.setOnErrorListener(null)
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (_: IllegalStateException) {
        } finally {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun maybeApplyStoredOfflineVoice() {
        if (useYandexCache) return
        val targetVoice = textToSpeech?.voices?.firstOrNull { it.name == currentVoiceId }

        if (targetVoice != null) {
            applyOfflineVoice(targetVoice)
        } else {
            val defaultVoice = textToSpeech?.voices?.firstOrNull {
                it.locale?.language?.startsWith("ru", ignoreCase = true) == true
            } ?: textToSpeech?.defaultVoice

            if (defaultVoice != null) {
                currentVoiceId = defaultVoice.name
                prefs.edit().putString(PREF_VOICE_URI, currentVoiceId).apply()
                applyOfflineVoice(defaultVoice)
            }
        }
    }

    private fun applyOfflineVoice(voice: Voice) {
        textToSpeech?.voice = voice
        eventsFlow.tryEmit(
            TtsEvent.Status("Выбран офлайн голос: ${voice.name} (${voice.locale})")
        )
    }

    private fun offlineVoiceComparator(): Comparator<TtsVoice> =
        Comparator { a, b ->
            val aRu = a.locale?.startsWith("ru", ignoreCase = true) == true
            val bRu = b.locale?.startsWith("ru", ignoreCase = true) == true
            when {
                aRu && !bRu -> -1
                bRu && !aRu -> 1
                else -> a.title.compareTo(b.title, ignoreCase = true)
            }
        }

    private fun resolveYandexVoiceId(requestedVoice: String?): String {
        val sanitized = requestedVoice
            ?.takeIf { it.isNotBlank() && it != "current" }
        val voices = getYandexVoices()
        val requested = sanitized?.let { id -> voices.firstOrNull { it.voiceURI == id } }?.voiceURI
        if (requested != null) return requested
        val stored = voices.firstOrNull { it.voiceURI == currentVoiceId }?.voiceURI
        return stored ?: DEFAULT_YANDEX_VOICE
    }

    private suspend fun writeTempFile(bytes: ByteArray): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(appContext.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
            file.outputStream().use { it.write(bytes) }
            file
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun saveToDownloads(bytes: ByteArray, text: String): String? =
        withContext(Dispatchers.IO) {
            val displayName = buildDownloadFileName(text)
            return@withContext try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                        put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LINKa")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = appContext.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { stream ->
                            stream.write(bytes)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        uri.toString()
                    } else {
                        null
                    }
                } else {
                    val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir == null) {
                        null
                    } else {
                        if (!downloadsDir.exists()) downloadsDir.mkdirs()
                        val file = File(downloadsDir, displayName)
                        FileOutputStream(file).use { it.write(bytes) }
                        file.absolutePath
                    }
                }
            } catch (_: IOException) {
                null
            }
        }

    private fun buildDownloadFileName(text: String): String {
        val raw = "LINKa_${text.take(32)}.mp3"
        val sanitized = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return sanitized.ifBlank { "tts_${System.currentTimeMillis()}.mp3" }
    }

    data class YandexVoice(
        val voiceURI: String,
        val text: String,
        val langCode: String? = null,
        val lang: String? = null,
        val gender: String? = null,
        val role: List<String>? = null
    )

    data class TtsVoice(
        val voiceId: String,
        val title: String,
        val locale: String? = null,
        val provider: VoiceProvider,
        val isDefault: Boolean = false
    )

    enum class VoiceProvider {
        YANDEX,
        OFFLINE
    }

    sealed class TtsEvent {
        object SpeakingStarted : TtsEvent()
        object SpeakingCompleted : TtsEvent()
        data class Error(val message: String) : TtsEvent()
        data class Status(val message: String) : TtsEvent()
        data class TemporarilyUnavailable(val message: String) : TtsEvent()
        object DownloadStarted : TtsEvent()
        data class DownloadProgress(val current: Int, val total: Int) : TtsEvent()
        data class DownloadCompleted(val path: String?) : TtsEvent()
    }

    companion object {
        private const val PREF_VOLUME = "volume"
        private const val PREF_RATE = "rate"
        private const val PREF_PITCH = "pitch"
        private const val PREF_USE_YANDEX = "yandex"
        private const val PREF_VOICE_URI = "voiceuri"
        private const val DEFAULT_YANDEX_VOICE = "zahar"
        private const val TTS_ENDPOINT = "https://tts.linka.su/tts"
        private const val VOICES_ENDPOINT = "https://tts.linka.su/voices"

        private val YANDEX_VOICES = listOf(
            YandexVoice(voiceURI = "zahar", text = "Захар"),
            YandexVoice(voiceURI = "ermil", text = "Ермил"),
            YandexVoice(voiceURI = "jane", text = "Джейн"),
            YandexVoice(voiceURI = "oksana", text = "Оксана"),
            YandexVoice(voiceURI = "alena", text = "Алёна"),
            YandexVoice(voiceURI = "filipp", text = "Филипп"),
            YandexVoice(voiceURI = "omazh", text = "Ома")
        )

        @VisibleForTesting
        internal fun YandexVoice.toTtsVoice(): TtsVoice =
            TtsVoice(
                voiceId = voiceURI,
                title = text,
                provider = VoiceProvider.YANDEX,
                isDefault = voiceURI == DEFAULT_YANDEX_VOICE
            )
    }
}
