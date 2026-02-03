package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.UserPreferences
import ru.ibakaidov.distypepro.shared.model.UserState
import ru.ibakaidov.distypepro.shared.model.UserStateUpdateRequest
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor
import ru.ibakaidov.distypepro.shared.sync.OfflineUserStatePayload
import ru.ibakaidov.distypepro.shared.utils.currentTimeMillis

class UserStateRepositoryImpl(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
) : UserStateRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getState(): UserState {
        return try {
            val remote = apiClient.authorizedRequest<UserState>(HttpMethod.Get, "/v1/user/state")
            val normalized = remote.copy(quickes = normalizeQuickes(remote.quickes))
            localStore.upsertUserState(normalized)
            normalized
        } catch (_: Exception) {
            val cached = localStore.getUserState()
            val quickes = cached?.quickes?.let { normalizeQuickes(it) } ?: normalizeQuickes(emptyList())
            cached?.copy(quickes = quickes) ?: UserState(inited = false, quickes = quickes, preferences = null)
        }
    }

    override suspend fun updateState(
        inited: Boolean?,
        quickes: List<String>?,
        preferences: UserPreferences?,
    ): UserState {
        val current = localStore.getUserState() ?: UserState()
        val normalizedQuickes = quickes?.let { normalizeQuickes(it) }
        val merged = current.copy(
            inited = inited ?: current.inited,
            quickes = normalizedQuickes ?: normalizeQuickes(current.quickes),
            preferences = preferences ?: current.preferences,
        )
        localStore.upsertUserState(merged)

        return try {
            val remote = apiClient.authorizedRequest<UserState>(
                HttpMethod.Put,
                "/v1/user/state",
                UserStateUpdateRequest(
                    inited = inited,
                    quickes = normalizedQuickes,
                    preferences = preferences,
                ),
            )
            val normalized = remote.copy(quickes = normalizeQuickes(remote.quickes))
            localStore.upsertUserState(normalized)
            normalized
        } catch (_: Exception) {
            val payload = OfflineUserStatePayload(inited = inited, quickes = normalizedQuickes, preferences = preferences)
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_USER_STATE,
                opType = OfflineQueueProcessor.OP_UPDATE,
                payload = json.encodeToString(OfflineUserStatePayload.serializer(), payload),
                createdAt = currentTimeMillis(),
            )
            merged
        }
    }

    private fun normalizeQuickes(values: List<String>): List<String> {
        if (values.size == QUICKES_SLOTS) return values
        val normalized = values.take(QUICKES_SLOTS).toMutableList()
        while (normalized.size < QUICKES_SLOTS) {
            normalized.add("")
        }
        return normalized
    }

    private companion object {
        private const val QUICKES_SLOTS = 6
    }
}
