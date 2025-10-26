package ru.ibakaidov.distypepro.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.io.File
import java.util.Locale
import java.util.UUID

class Tts(context: Context, locale: Locale = Locale.getDefault()) {
    private val appContext = context.applicationContext
    private val cacheDir = appContext.cacheDir
    private val textToSpeech: TextToSpeech = TextToSpeech(appContext) { status ->
        onInitCallback?.onDone(status)
    }.apply {
        language = locale
    }

    private var onInitCallback: Callback<Int>? = null
    private var onPlayCallback: Callback<ProgressState>? = null

    fun setOnPlayCallback(callback: Callback<ProgressState>) {
        onPlayCallback = callback
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onPlayCallback?.onDone(ProgressState.START)
            }

            override fun onDone(utteranceId: String?) {
                onPlayCallback?.onDone(ProgressState.STOP)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                // For now we only surface STOP to match legacy behaviour.
                onPlayCallback?.onDone(ProgressState.STOP)
            }
        })
    }

    fun setOnInitCallback(callback: Callback<Int>) {
        onInitCallback = callback
    }

    fun getVoices(): Set<Voice> = textToSpeech.voices

    fun speak(text: String) {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            onPlayCallback?.onDone(ProgressState.STOP)
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    @Throws(Exception::class)
    fun speakToBuffer(text: String): File {
        val file = File(cacheDir, "${UUID.randomUUID()}.wav")
        val result = textToSpeech.synthesizeToFile(text, null, file, null)
        if (result != TextToSpeech.SUCCESS) throw Exception("synth error")
        return file
    }

    fun shutdown() {
        textToSpeech.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "utterance"
    }
}
