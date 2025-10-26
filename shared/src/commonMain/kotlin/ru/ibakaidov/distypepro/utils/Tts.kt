package ru.ibakaidov.distypepro.utils

expect class Tts {
    fun speak(text: String)
    fun stop()
    fun isSpeaking(): Boolean
}