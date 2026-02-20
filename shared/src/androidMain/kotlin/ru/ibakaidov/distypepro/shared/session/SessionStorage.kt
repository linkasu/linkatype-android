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

    private companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
