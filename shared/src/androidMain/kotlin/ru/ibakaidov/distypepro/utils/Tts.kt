package ru.ibakaidov.distypepro.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

actual class Tts(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isCurrentlySpeaking = false

    init {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.setLanguage(Locale.getDefault())
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isCurrentlySpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isCurrentlySpeaking = false
                    }

                    override fun onError(utteranceId: String?) {
                        isCurrentlySpeaking = false
                    }
                })
            }
        }
    }

    actual fun speak(text: String) {
        if (isInitialized && !isCurrentlySpeaking) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    actual fun stop() {
        tts?.stop()
        isCurrentlySpeaking = false
    }

    actual fun isSpeaking(): Boolean = isCurrentlySpeaking

    fun shutdown() {
        tts?.shutdown()
    }
}