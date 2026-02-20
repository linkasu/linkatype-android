package ru.ibakaidov.distypepro.shared.session

import ru.ibakaidov.distypepro.shared.utils.generateId

interface SessionRepository {
    fun getMode(): AppMode?
    fun setMode(mode: AppMode?)
    fun getOrCreateDeviceId(): String

    fun clearMode() {
        setMode(mode = null)
    }

    fun getSessionInfo(): SessionInfo {
        return SessionInfo(
            mode = getMode(),
            deviceId = getOrCreateDeviceId(),
        )
    }
}

class DefaultSessionRepository(
    private val storage: SessionStorage,
    private val idGenerator: () -> String = { "device_${generateId()}" },
) : SessionRepository {
    override fun getMode(): AppMode? {
        return when (storage.getMode()?.lowercase()) {
            "online" -> AppMode.ONLINE
            "offline" -> AppMode.OFFLINE
            else -> null
        }
    }

    override fun setMode(mode: AppMode?) {
        val value = when (mode) {
            AppMode.ONLINE -> "online"
            AppMode.OFFLINE -> "offline"
            null -> null
        }
        storage.setMode(value)
    }

    override fun getOrCreateDeviceId(): String {
        val existing = storage.getDeviceId()
        if (!existing.isNullOrBlank()) return existing

        val created = idGenerator()
        storage.setDeviceId(created)
        return created
    }
}

class InMemorySessionRepository(
    private var mode: AppMode = AppMode.ONLINE,
    private var deviceId: String = "device_test",
) : SessionRepository {
    override fun getMode(): AppMode = mode

    override fun setMode(mode: AppMode?) {
        if (mode != null) {
            this.mode = mode
        }
    }

    override fun clearMode() {
        // Keep ONLINE default for test doubles.
        mode = AppMode.ONLINE
    }

    override fun getOrCreateDeviceId(): String = deviceId
}
