package ru.ibakaidov.distypepro.shared.sync

import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.model.CategoryCreateRequest
import ru.ibakaidov.distypepro.shared.model.CategoryUpdateRequest
import ru.ibakaidov.distypepro.shared.model.DialogMessageRequest
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.model.StatementCreateRequest
import ru.ibakaidov.distypepro.shared.model.StatementUpdateRequest
import ru.ibakaidov.distypepro.shared.model.UserState
import ru.ibakaidov.distypepro.shared.model.UserStateUpdateRequest

class OfflineQueueProcessor(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val flushMutex = Mutex()

    suspend fun flush() {
        flushMutex.withLock {
            val queue = localStore.listOfflineQueue()
            for (entry in queue) {
                try {
                    process(entry.entityType, entry.opType, entry.payload)
                    localStore.deleteOfflineEntry(entry.id)
                } catch (ex: Exception) {
                    localStore.updateOfflineRetry(entry.id, entry.retryCount + 1, ex.message)
                }
            }
        }
    }

    private suspend fun process(entityType: String, opType: String, payload: String) {
        when (entityType) {
            ENTITY_CATEGORY -> handleCategory(opType, payload)
            ENTITY_STATEMENT -> handleStatement(opType, payload)
            ENTITY_QUICKES -> handleQuickes(opType, payload)
            ENTITY_USER_STATE -> handleUserState(opType, payload)
            ENTITY_DIALOG_MESSAGE -> handleDialogMessage(payload)
        }
    }

    private suspend fun handleCategory(opType: String, payload: String) {
        when (opType) {
            OP_CREATE -> {
                val data = json.decodeFromString(OfflineCategoryPayload.serializer(), payload)
                val created = apiClient.authorizedRequest<Category>(
                    HttpMethod.Post,
                    "/v1/categories",
                    CategoryCreateRequest(
                        id = data.id,
                        label = data.label,
                        created = data.created,
                        aiUse = data.aiUse,
                    ),
                )
                localStore.upsertCategory(created)
            }

            OP_UPDATE -> {
                val data = json.decodeFromString(OfflineCategoryUpdatePayload.serializer(), payload)
                val updated = apiClient.authorizedRequest<Category>(
                    HttpMethod.Patch,
                    "/v1/categories/${data.id}",
                    CategoryUpdateRequest(
                        label = data.label,
                        aiUse = data.aiUse,
                    ),
                )
                localStore.upsertCategory(updated)
            }

            OP_DELETE -> {
                val data = json.decodeFromString(OfflineCategoryUpdatePayload.serializer(), payload)
                apiClient.authorizedRequest<Unit>(
                    HttpMethod.Delete,
                    "/v1/categories/${data.id}",
                )
                localStore.deleteCategory(data.id)
            }
        }
    }

    private suspend fun handleStatement(opType: String, payload: String) {
        when (opType) {
            OP_CREATE -> {
                val data = json.decodeFromString(OfflineStatementPayload.serializer(), payload)
                val created = apiClient.authorizedRequest<Statement>(
                    HttpMethod.Post,
                    "/v1/statements",
                    StatementCreateRequest(
                        id = data.id,
                        categoryId = data.categoryId,
                        text = data.text,
                        created = data.created,
                    ),
                )
                localStore.upsertStatement(created)
            }

            OP_UPDATE -> {
                val data = json.decodeFromString(OfflineStatementUpdatePayload.serializer(), payload)
                val updated = apiClient.authorizedRequest<Statement>(
                    HttpMethod.Patch,
                    "/v1/statements/${data.id}",
                    StatementUpdateRequest(text = data.text),
                )
                localStore.upsertStatement(updated)
            }

            OP_DELETE -> {
                val data = json.decodeFromString(OfflineStatementUpdatePayload.serializer(), payload)
                apiClient.authorizedRequest<Unit>(
                    HttpMethod.Delete,
                    "/v1/statements/${data.id}",
                )
                localStore.deleteStatement(data.id)
            }
        }
    }

    private suspend fun handleQuickes(opType: String, payload: String) {
        if (opType != OP_SET) return
        val data = json.decodeFromString(OfflineQuickesPayload.serializer(), payload)
        val updated = apiClient.authorizedRequest<List<String>>(
            HttpMethod.Put,
            "/v1/quickes",
            mapOf("quickes" to data.quickes),
        )
        localStore.replaceQuickes(updated)
    }

    private suspend fun handleUserState(opType: String, payload: String) {
        if (opType != OP_UPDATE) return
        val data = json.decodeFromString(OfflineUserStatePayload.serializer(), payload)
        val updated = apiClient.authorizedRequest<UserState>(
            HttpMethod.Put,
            "/v1/user/state",
            UserStateUpdateRequest(
                inited = data.inited,
                quickes = data.quickes,
                preferences = data.preferences,
            ),
        )
        localStore.upsertUserState(updated)
    }

    private suspend fun handleDialogMessage(payload: String) {
        val data = json.decodeFromString(OfflineDialogMessagePayload.serializer(), payload)
        val request = DialogMessageRequest(
            role = data.role,
            content = data.content,
            source = data.source,
            created = data.created,
            includeSuggestions = data.includeSuggestions,
        )
        apiClient.authorizedRequest<Unit>(
            HttpMethod.Post,
            "/v1/dialog/chats/${data.chatId}/messages",
            request,
        )
    }

    companion object {
        const val ENTITY_CATEGORY = "category"
        const val ENTITY_STATEMENT = "statement"
        const val ENTITY_QUICKES = "quickes"
        const val ENTITY_USER_STATE = "user_state"
        const val ENTITY_DIALOG_MESSAGE = "dialog_message"

        const val OP_CREATE = "create"
        const val OP_UPDATE = "update"
        const val OP_DELETE = "delete"
        const val OP_SET = "set"
    }
}
