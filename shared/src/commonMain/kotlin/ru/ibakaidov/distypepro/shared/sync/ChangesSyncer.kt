package ru.ibakaidov.distypepro.shared.sync

import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.model.ChangeEvent
import ru.ibakaidov.distypepro.shared.model.ChangesResponse
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.model.UserState

class ChangesSyncer(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Throws(Exception::class)
    suspend fun pollOnce(limit: Int = 100, timeoutSeconds: Int = 25): ChangesResponse {
        val cursor = localStore.getSyncValue(KEY_CURSOR).orEmpty()
        val queryCursor = if (cursor.isBlank()) "" else cursor.encodeURLParameter()
        val path = buildString {
            append("/v1/changes?limit=")
            append(limit)
            append("&timeout=")
            append(timeoutSeconds)
            append("s")
            if (queryCursor.isNotBlank()) {
                append("&cursor=")
                append(queryCursor)
            }
        }
        val response = apiClient.authorizedRequest<ChangesResponse>(HttpMethod.Get, path)
        if (response.changes.isNotEmpty()) {
            applyChanges(response.changes)
        }
        if (response.cursor.isNotBlank()) {
            localStore.setSyncValue(KEY_CURSOR, response.cursor)
        }
        return response
    }

    private fun applyChanges(changes: List<ChangeEvent>) {
        changes.forEach { change ->
            when (change.entityType) {
                ENTITY_CATEGORY -> applyCategory(change)
                ENTITY_STATEMENT -> applyStatement(change)
                ENTITY_QUICKES -> applyQuickes(change)
                ENTITY_USER_STATE -> applyUserState(change)
                else -> Unit
            }
        }
    }

    private fun applyCategory(change: ChangeEvent) {
        when (change.op) {
            OP_DELETE -> {
                val id = payloadId(change.payload) ?: change.entityId
                if (id.isBlank()) return
                val local = localStore.findCategory(id)
                val localUpdated = local?.updatedAt ?: local?.created ?: 0L
                if (local == null || localUpdated <= change.updatedAt) {
                    localStore.deleteCategory(id)
                }
            }
            else -> {
                val category = decode<Category>(change.payload) ?: return
                val local = localStore.findCategory(category.id)
                val incomingUpdated = category.updatedAt ?: category.created
                val localUpdated = local?.updatedAt ?: local?.created ?: 0L
                if (incomingUpdated >= localUpdated) {
                    localStore.upsertCategory(category)
                }
            }
        }
    }

    private fun applyStatement(change: ChangeEvent) {
        when (change.op) {
            OP_DELETE -> {
                val id = payloadId(change.payload) ?: change.entityId
                if (id.isBlank()) return
                val local = localStore.findStatement(id)
                val localUpdated = local?.updatedAt ?: local?.created ?: 0L
                if (local == null || localUpdated <= change.updatedAt) {
                    localStore.deleteStatement(id)
                }
            }
            else -> {
                val statement = decode<Statement>(change.payload) ?: return
                val local = localStore.findStatement(statement.id)
                val incomingUpdated = statement.updatedAt ?: statement.created
                val localUpdated = local?.updatedAt ?: local?.created ?: 0L
                if (incomingUpdated >= localUpdated) {
                    localStore.upsertStatement(statement)
                }
            }
        }
    }

    private fun applyQuickes(change: ChangeEvent) {
        if (change.op != OP_UPSERT) return
        if (!shouldApplyTimestamp(KEY_QUICKES_UPDATED_AT, change.updatedAt)) return
        val quickes = decodeQuickes(change.payload) ?: return
        localStore.replaceQuickes(quickes)
    }

    private fun applyUserState(change: ChangeEvent) {
        if (change.op != OP_UPSERT) return
        if (!shouldApplyTimestamp(KEY_USER_STATE_UPDATED_AT, change.updatedAt)) return
        val state = decode<UserState>(change.payload) ?: return
        localStore.upsertUserState(state)
    }

    private fun shouldApplyTimestamp(key: String, updatedAt: Long): Boolean {
        val current = localStore.getSyncLong(key) ?: Long.MIN_VALUE
        return if (updatedAt >= current) {
            localStore.setSyncLong(key, updatedAt)
            true
        } else {
            false
        }
    }

    private fun payloadId(payload: JsonElement): String? {
        return runCatching { payload.jsonObject["id"]?.jsonPrimitive?.content }.getOrNull()
    }

    private fun decodeQuickes(payload: JsonElement): List<String>? {
        val direct = runCatching {
            json.decodeFromJsonElement(ListSerializer(String.serializer()), payload)
        }.getOrNull()
        if (direct != null) return direct
        val quickes = runCatching { payload.jsonObject["quickes"] }.getOrNull() ?: return null
        return runCatching {
            json.decodeFromJsonElement(ListSerializer(String.serializer()), quickes)
        }.getOrNull()
    }

    private inline fun <reified T> decode(payload: JsonElement): T? {
        return runCatching { json.decodeFromJsonElement<T>(payload) }.getOrNull()
    }

    companion object {
        private const val ENTITY_CATEGORY = "category"
        private const val ENTITY_STATEMENT = "statement"
        private const val ENTITY_QUICKES = "quickes"
        private const val ENTITY_USER_STATE = "user_state"

        private const val OP_UPSERT = "upsert"
        private const val OP_DELETE = "delete"

        private const val KEY_CURSOR = "realtime_cursor"
        private const val KEY_QUICKES_UPDATED_AT = "quickes_updated_at"
        private const val KEY_USER_STATE_UPDATED_AT = "user_state_updated_at"
    }
}
