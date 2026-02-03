package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.shared.model.DialogMessage
import ru.ibakaidov.distypepro.shared.model.DialogMessageResult
import ru.ibakaidov.distypepro.shared.model.DialogMessageRequest
import ru.ibakaidov.distypepro.shared.model.DialogSuggestion
import ru.ibakaidov.distypepro.shared.model.DialogSuggestionApplyItem
import ru.ibakaidov.distypepro.shared.model.DialogSuggestionApplyResult
import ru.ibakaidov.distypepro.shared.sync.OfflineDialogMessagePayload
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor
import ru.ibakaidov.distypepro.shared.utils.currentTimeMillis
import ru.ibakaidov.distypepro.shared.utils.generateId

class DialogRepositoryImpl(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
    private val now: () -> Long = { currentTimeMillis() },
) : DialogRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listChats(): List<DialogChat> {
        return try {
            val remote = apiClient.authorizedRequest<List<DialogChat>>(HttpMethod.Get, "/v1/dialog/chats")
            remote.forEach { localStore.upsertChat(it) }
            remote
        } catch (_: Exception) {
            localStore.listChats()
        }
    }

    override suspend fun createChat(title: String?): DialogChat {
        return try {
            val remote = apiClient.authorizedRequest<DialogChat>(
                HttpMethod.Post,
                "/v1/dialog/chats",
                mapOf("title" to title),
            )
            localStore.upsertChat(remote)
            remote
        } catch (_: Exception) {
            val optimistic = DialogChat(
                id = generateId(),
                title = title,
                created = now(),
            )
            localStore.upsertChat(optimistic)
            optimistic
        }
    }

    override suspend fun deleteChat(id: String) {
        localStore.deleteChat(id)
        try {
            apiClient.authorizedRequest<Unit>(HttpMethod.Delete, "/v1/dialog/chats/$id")
        } catch (_: Exception) {
            // Best-effort; chats are ephemeral.
        }
    }

    override suspend fun listMessages(chatId: String, limit: Int?, before: Long?): List<DialogMessage> {
        return try {
            val query = buildMap<String, Any> {
                if (limit != null) put("limit", limit)
                if (before != null) put("before", before)
            }
            val remote = apiClient.authorizedRequest<List<DialogMessage>>(
                HttpMethod.Get,
                "/v1/dialog/chats/$chatId/messages" + query.toQueryString(),
            )
            remote.forEach { localStore.upsertMessage(it) }
            if (hasPendingDialogMessages(chatId)) {
                val local = localStore.listMessages(chatId, (limit ?: 200).toLong())
                mergeMessages(remote, local)
            } else {
                remote
            }
        } catch (_: Exception) {
            localStore.listMessages(chatId, (limit ?: 200).toLong())
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        role: String,
        content: String,
        source: String?,
        created: Long?,
        includeSuggestions: Boolean?,
    ): DialogMessageResult {
        val createdAt = created ?: now()
        val optimistic = DialogMessage(
            id = generateId(),
            chatId = chatId,
            role = role,
            content = content,
            source = source,
            created = createdAt,
        )
        localStore.upsertMessage(optimistic)
        val request = DialogMessageRequest(
            role = role,
            content = content,
            source = source,
            created = createdAt,
            includeSuggestions = includeSuggestions,
        )

        return try {
            val result = apiClient.authorizedRequest<DialogMessageResult>(
                HttpMethod.Post,
                "/v1/dialog/chats/$chatId/messages",
                request,
            )
            localStore.upsertMessage(result.message)
            result
        } catch (_: Exception) {
            val payload = OfflineDialogMessagePayload(
                chatId = chatId,
                role = role,
                content = content,
                source = source,
                created = createdAt,
                includeSuggestions = includeSuggestions,
            )
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_DIALOG_MESSAGE,
                opType = OfflineQueueProcessor.OP_CREATE,
                payload = json.encodeToString(OfflineDialogMessagePayload.serializer(), payload),
                createdAt = createdAt,
            )
            DialogMessageResult(message = optimistic, transcript = null, suggestions = null)
        }
    }

    override suspend fun sendAudioMessage(
        chatId: String,
        role: String,
        audioBytes: ByteArray,
        mimeType: String,
        filename: String,
        created: Long?,
        source: String?,
        includeSuggestions: Boolean?,
    ): DialogMessageResult {
        val createdAt = created ?: now()
        val payloadJson = json.encodeToString(
            OfflineDialogMessagePayload.serializer(),
            OfflineDialogMessagePayload(
                chatId = chatId,
                role = role,
                content = "",
                source = source,
                created = createdAt,
                includeSuggestions = includeSuggestions,
            ),
        )
        return apiClient.postMultipartAuthorized(
            path = "/v1/dialog/chats/$chatId/messages",
            payloadJson = payloadJson,
            audioBytes = audioBytes,
            filename = filename,
            mimeType = mimeType,
        )
    }

    override suspend fun listSuggestions(status: String, limit: Int): List<DialogSuggestion> {
        return try {
            val remote = apiClient.authorizedRequest<List<DialogSuggestion>>(
                HttpMethod.Get,
                "/v1/dialog/suggestions?status=$status&limit=$limit",
            )
            remote.forEach { localStore.upsertSuggestion(it) }
            remote
        } catch (_: Exception) {
            localStore.listSuggestions(status, limit.toLong())
        }
    }

    override suspend fun applySuggestions(items: List<DialogSuggestionApplyItem>): DialogSuggestionApplyResult {
        return apiClient.authorizedRequest(
            HttpMethod.Post,
            "/v1/dialog/suggestions/apply",
            mapOf("items" to items),
        )
    }

    override suspend fun dismissSuggestions(ids: List<String>) {
        apiClient.authorizedRequest<Unit>(
            HttpMethod.Post,
            "/v1/dialog/suggestions/dismiss",
            mapOf("ids" to ids),
        )
        ids.forEach { localStore.removeSuggestion(it) }
    }

    private fun Map<String, Any>.toQueryString(): String {
        if (isEmpty()) return ""
        return entries.joinToString(prefix = "?", separator = "&") { (key, value) ->
            "$key=$value"
        }
    }

    private fun hasPendingDialogMessages(chatId: String): Boolean {
        return localStore.listOfflineQueue()
            .asSequence()
            .filter { it.entityType == OfflineQueueProcessor.ENTITY_DIALOG_MESSAGE }
            .mapNotNull {
                runCatching { json.decodeFromString(OfflineDialogMessagePayload.serializer(), it.payload) }.getOrNull()
            }
            .any { it.chatId == chatId }
    }

    private fun mergeMessages(remote: List<DialogMessage>, local: List<DialogMessage>): List<DialogMessage> {
        if (local.isEmpty()) return remote
        return (remote + local)
            .distinctBy { Triple(it.role, it.created, it.content) }
            .sortedBy { it.created }
    }
}
