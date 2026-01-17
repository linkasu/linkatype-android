package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor
import ru.ibakaidov.distypepro.shared.sync.OfflineStatementPayload
import ru.ibakaidov.distypepro.shared.sync.OfflineStatementUpdatePayload
import ru.ibakaidov.distypepro.shared.utils.currentTimeMillis
import ru.ibakaidov.distypepro.shared.utils.generateId

class StatementsRepositoryImpl(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
    private val now: () -> Long = { currentTimeMillis() },
) : StatementsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listByCategory(categoryId: String): List<Statement> {
        return try {
            val remote = apiClient.authorizedRequest<List<Statement>>(
                HttpMethod.Get,
                "/v1/categories/$categoryId/statements",
            )
            remote.forEach { localStore.upsertStatement(it) }
            remote
        } catch (_: Exception) {
            localStore.listStatements(categoryId)
        }
    }

    override suspend fun create(categoryId: String, text: String, created: Long?): Statement {
        val createdAt = created ?: now()
        val id = generateId()
        val optimistic = Statement(
            id = id,
            categoryId = categoryId,
            text = text,
            created = createdAt,
        )
        localStore.upsertStatement(optimistic)

        return try {
            val remote = apiClient.authorizedRequest<Statement>(
                HttpMethod.Post,
                "/v1/statements",
                mapOf(
                    "id" to id,
                    "categoryId" to categoryId,
                    "text" to text,
                    "created" to createdAt,
                ),
            )
            localStore.upsertStatement(remote)
            remote
        } catch (_: Exception) {
            val payload = OfflineStatementPayload(
                id = id,
                categoryId = categoryId,
                text = text,
                created = createdAt,
            )
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_STATEMENT,
                opType = OfflineQueueProcessor.OP_CREATE,
                payload = json.encodeToString(OfflineStatementPayload.serializer(), payload),
                createdAt = createdAt,
            )
            optimistic
        }
    }

    override suspend fun update(id: String, text: String): Statement {
        val current = localStore.findStatement(id)
        val optimistic = Statement(
            id = id,
            categoryId = current?.categoryId.orEmpty(),
            text = text,
            created = current?.created ?: now(),
            updatedAt = now(),
        )
        localStore.upsertStatement(optimistic)

        return try {
            val remote = apiClient.authorizedRequest<Statement>(
                HttpMethod.Patch,
                "/v1/statements/$id",
                mapOf("text" to text),
            )
            localStore.upsertStatement(remote)
            remote
        } catch (_: Exception) {
            val payload = OfflineStatementUpdatePayload(id = id, text = text)
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_STATEMENT,
                opType = OfflineQueueProcessor.OP_UPDATE,
                payload = json.encodeToString(OfflineStatementUpdatePayload.serializer(), payload),
                createdAt = now(),
            )
            optimistic
        }
    }

    override suspend fun delete(id: String) {
        localStore.deleteStatement(id)
        try {
            apiClient.authorizedRequest<Unit>(HttpMethod.Delete, "/v1/statements/$id")
        } catch (_: Exception) {
            val payload = OfflineStatementUpdatePayload(id = id, text = "")
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_STATEMENT,
                opType = OfflineQueueProcessor.OP_DELETE,
                payload = json.encodeToString(OfflineStatementUpdatePayload.serializer(), payload),
                createdAt = now(),
            )
        }
    }
}
