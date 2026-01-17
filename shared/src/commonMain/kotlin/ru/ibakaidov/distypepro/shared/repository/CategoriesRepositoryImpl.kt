package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.sync.OfflineCategoryPayload
import ru.ibakaidov.distypepro.shared.sync.OfflineCategoryUpdatePayload
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor
import ru.ibakaidov.distypepro.shared.utils.currentTimeMillis
import ru.ibakaidov.distypepro.shared.utils.generateId

class CategoriesRepositoryImpl(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
    private val now: () -> Long = { currentTimeMillis() },
) : CategoriesRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun list(): List<Category> {
        return try {
            val remote = apiClient.authorizedRequest<List<Category>>(HttpMethod.Get, "/v1/categories")
            remote.forEach { localStore.upsertCategory(it) }
            remote
        } catch (_: Exception) {
            localStore.listCategories()
        }
    }

    override suspend fun create(label: String, created: Long?, aiUse: Boolean?): Category {
        val createdAt = created ?: now()
        val id = generateId()
        val optimistic = Category(
            id = id,
            label = label,
            created = createdAt,
            aiUse = aiUse ?: false,
        )
        localStore.upsertCategory(optimistic)

        return try {
            val remote = apiClient.authorizedRequest<Category>(
                HttpMethod.Post,
                "/v1/categories",
                mapOf(
                    "id" to id,
                    "label" to label,
                    "created" to createdAt,
                    "aiUse" to (aiUse ?: false),
                ),
            )
            localStore.upsertCategory(remote)
            remote
        } catch (ex: Exception) {
            val payload = OfflineCategoryPayload(
                id = id,
                label = label,
                created = createdAt,
                aiUse = aiUse ?: false,
            )
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_CATEGORY,
                opType = OfflineQueueProcessor.OP_CREATE,
                payload = json.encodeToString(OfflineCategoryPayload.serializer(), payload),
                createdAt = createdAt,
            )
            optimistic
        }
    }

    override suspend fun update(id: String, label: String?, aiUse: Boolean?): Category {
        val current = localStore.listCategories().firstOrNull { it.id == id }
        val optimistic = Category(
            id = id,
            label = label ?: current?.label.orEmpty(),
            created = current?.created ?: now(),
            default = current?.default ?: false,
            aiUse = aiUse ?: current?.aiUse ?: false,
            updatedAt = now(),
        )
        localStore.upsertCategory(optimistic)

        return try {
            val remote = apiClient.authorizedRequest<Category>(
                HttpMethod.Patch,
                "/v1/categories/$id",
                mapOf(
                    "label" to label,
                    "aiUse" to aiUse,
                ),
            )
            localStore.upsertCategory(remote)
            remote
        } catch (_: Exception) {
            val payload = OfflineCategoryUpdatePayload(
                id = id,
                label = label,
                aiUse = aiUse,
            )
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_CATEGORY,
                opType = OfflineQueueProcessor.OP_UPDATE,
                payload = json.encodeToString(OfflineCategoryUpdatePayload.serializer(), payload),
                createdAt = now(),
            )
            optimistic
        }
    }

    override suspend fun delete(id: String) {
        localStore.deleteCategory(id)
        try {
            apiClient.authorizedRequest<Unit>(HttpMethod.Delete, "/v1/categories/$id")
        } catch (_: Exception) {
            val payload = OfflineCategoryUpdatePayload(id = id)
            localStore.enqueueOffline(
                entityType = OfflineQueueProcessor.ENTITY_CATEGORY,
                opType = OfflineQueueProcessor.OP_DELETE,
                payload = json.encodeToString(OfflineCategoryUpdatePayload.serializer(), payload),
                createdAt = now(),
            )
        }
    }
}
