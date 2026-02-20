package ru.ibakaidov.distypepro.shared.session

import ru.ibakaidov.distypepro.shared.auth.PlatformContext

expect class SessionStorage(context: PlatformContext) {
    fun getMode(): String?
    fun setMode(value: String?)
    fun getDeviceId(): String?
    fun setDeviceId(value: String?)
}
