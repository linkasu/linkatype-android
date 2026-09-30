package ru.ibakaidov.distypepro.shared.session

import platform.Foundation.NSUserDefaults
import ru.ibakaidov.distypepro.shared.auth.PlatformContext

actual class SessionStorage actual constructor(context: PlatformContext) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getMode(): String? = defaults.stringForKey(KEY_MODE)

    actual fun setMode(value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(KEY_MODE)
        } else {
            defaults.setObject(value, forKey = KEY_MODE)
        }
        defaults.synchronize()
    }

    actual fun getDeviceId(): String? = defaults.stringForKey(KEY_DEVICE_ID)

    actual fun setDeviceId(value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(KEY_DEVICE_ID)
        } else {
            defaults.setObject(value, forKey = KEY_DEVICE_ID)
        }
        defaults.synchronize()
    }

    actual fun getTtsInstallationToken(): String? = defaults.stringForKey(KEY_TTS_INSTALLATION_TOKEN)

    actual fun getTtsInstallationTokenExpiresAtMillis(): Long? =
        defaults.stringForKey(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT)?.toLongOrNull()

    actual fun setTtsInstallationToken(token: String?, expiresAtMillis: Long?) {
        if (token == null || expiresAtMillis == null) {
            defaults.removeObjectForKey(KEY_TTS_INSTALLATION_TOKEN)
            defaults.removeObjectForKey(KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT)
        } else {
            defaults.setObject(token, forKey = KEY_TTS_INSTALLATION_TOKEN)
            defaults.setObject(expiresAtMillis.toString(), forKey = KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT)
        }
        defaults.synchronize()
    }

    private companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TTS_INSTALLATION_TOKEN = "tts_installation_token"
        private const val KEY_TTS_INSTALLATION_TOKEN_EXPIRES_AT = "tts_installation_token_expires_at"
    }
}
