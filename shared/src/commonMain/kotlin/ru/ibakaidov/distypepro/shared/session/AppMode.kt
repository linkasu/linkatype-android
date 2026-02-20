package ru.ibakaidov.distypepro.shared.session

enum class AppMode {
    ONLINE,
    OFFLINE,
}

data class SessionInfo(
    val mode: AppMode?,
    val deviceId: String?,
)
