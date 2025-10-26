package ru.ibakaidov.distypepro.utils

import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance
import platform.AVFoundation.AVSpeechSynthesisVoice
import platform.Foundation.NSLocale

actual class Tts {
    private val synthesizer = AVSpeechSynthesizer()
    private var isCurrentlySpeaking = false

    actual fun speak(text: String) {
        if (!isCurrentlySpeaking) {
            val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(NSLocale.currentLocale().languageCode)
            synthesizer.speakUtterance(utterance)
            isCurrentlySpeaking = true
        }
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(0)
        isCurrentlySpeaking = false
    }

    actual fun isSpeaking(): Boolean = isCurrentlySpeaking
}