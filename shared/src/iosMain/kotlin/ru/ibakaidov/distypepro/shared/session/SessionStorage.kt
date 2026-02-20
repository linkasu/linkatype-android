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

    private companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
