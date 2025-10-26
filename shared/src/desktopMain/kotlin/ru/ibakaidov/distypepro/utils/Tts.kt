package ru.ibakaidov.distypepro.utils

actual class Tts {
    private var isCurrentlySpeaking = false

    actual fun speak(text: String) {
        // Для desktop платформ можно использовать системные TTS API
        // или внешние библиотеки. Пока заглушка.
        println("TTS: $text")
        isCurrentlySpeaking = true
        // Имитация завершения речи
        Thread {
            Thread.sleep(2000)
            isCurrentlySpeaking = false
        }.start()
    }

    actual fun stop() {
        isCurrentlySpeaking = false
    }

    actual fun isSpeaking(): Boolean = isCurrentlySpeaking
}