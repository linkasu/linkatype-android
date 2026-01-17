package ru.ibakaidov.distypepro.shared.auth

import platform.Foundation.NSUserDefaults

actual abstract class PlatformContext

class IosPlatformContext : PlatformContext()

actual class SecureTokenStorage actual constructor(context: PlatformContext) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun get(): String? = defaults.stringForKey(KEY_REFRESH_TOKEN)

    actual fun set(value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        } else {
            defaults.setObject(value, forKey = KEY_REFRESH_TOKEN)
        }
        defaults.synchronize()
    }

    actual fun clear() {
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.synchronize()
    }

    private companion object {
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
