package ru.ibakaidov.distypepro.utils

import android.content.Context

object TtsHolder {

    @Volatile
    private var instance: Tts? = null

    fun get(context: Context): Tts {
        val current = instance
        if (current != null) return current
        return synchronized(this) {
            val again = instance
            if (again != null) {
                again
            } else {
                val created = Tts(context.applicationContext)
                instance = created
                created
            }
        }
    }

    fun set(tts: Tts) {
        instance = tts
    }

    fun clear() {
        instance = null
    }
}
