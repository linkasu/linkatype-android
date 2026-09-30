package ru.ibakaidov.distypepro.shared.session

import ru.ibakaidov.distypepro.shared.auth.PlatformContext

actual class SessionStorage actual constructor(context: PlatformContext) {
    private val preferences = context.getSharedPreferences("linka_session_store", 0)

    actual fun getMode(): String? = preferences.getString(KEY_MODE, null)

    actual fun setMode(value: String?) {
        val editor = preferences.edit()
        if (value == null) {
            editor.remove(KEY_MODE)
        } else {
            editor.putString(KEY_MODE, value)
        }
        editor.apply()
    }

    actual fun getDeviceId(): String? = preferences.getString(KEY_DEVICE_ID, null)

    actual fun setDeviceId(value: String?) {
        val editor = preferences.edit()
        if (value == null) {
            editor.remove(KEY_DEVICE_ID)
        } else {
            editor.putString(KEY_DEVICE_ID, value)
        }
        editor.apply()
    }

    actual fun getTtsInstallationToken(): String? = preferences.getString(KEY_TTS_INSTALLATION_TOKEN, null)

    actual fun getTtsInstallationTokenExpiresAtMillis(): Long? =
        if (preferences.contains(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT)) {
            preferences.getLong(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT, 0L)
        } else {
            null
        }

    actual fun setTtsInstallationToken(token: String?, expiresAtMillis: Long?) {
        preferences.edit().apply {
            if (token == null || expiresAtMillis == null) {
                remove(KEY_TTS_INSTALLATION_TOKEN)
                remove(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT)
            } else {
                putString(KEY_TTS_INSTALLATION_TOKEN, token)
                putLong(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT, expiresAtMillis)
            }
        }.apply()
    }

    private companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TTS_INSTALLATION_TOKEN = "tts_installation_token"
        private const val KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT = "tts_installation_token_expires_at"
    }
}
