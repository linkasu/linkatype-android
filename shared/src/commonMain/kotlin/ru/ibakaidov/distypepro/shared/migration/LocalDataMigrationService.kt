package ru.ibakaidov.distypepro.shared.migration

import io.ktor.http.HttpMethod
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.model.CategoryCreateRequest
import ru.ibakaidov.distypepro.shared.model.CategoryUpdateRequest
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.model.StatementCreateRequest
import ru.ibakaidov.distypepro.shared.model.StatementUpdateRequest
import ru.ibakaidov.distypepro.shared.model.UserState
import ru.ibakaidov.distypepro.shared.model.UserStateUpdateRequest

class LocalDataMigrationService(
    private val apiClient: ApiClient,
    private val localStore: LocalStore,
) {
    suspend fun hasLocalDataForMigration(): Boolean = localStore.hasLocalUserData()

    suspend fun syncLocalDataToRemote() {
        val localCategories = localStore.listCategories()
        val localStatements = localCategories
            .flatMap { category -> localStore.listStatements(category.id) }
        val localState = localStore.getUserState()

        localCategories.forEach { category ->
            upsertCategoryRemote(category)
        }
        localStatements.forEach { statement ->
            upsertStatementRemote(statement)
        }

        if (localState != null) {
            val syncedState = apiClient.authorizedRequest<UserState>(
                HttpMethod.Put,
                "/v1/user/state",
                UserStateUpdateRequest(
                    inited = localState.inited,
                    quickes = localState.quickes,
                    preferences = localState.preferences,
                ),
            )
            localStore.upsertUserState(syncedState)
        }
    }

    suspend fun replaceLocalDataWithRemote() {
        val remoteCategories = apiClient.authorizedRequest<List<Category>>(
            HttpMethod.Get,
            "/v1/categories",
        )
        val remoteStatements = remoteCategories.flatMap { category ->
            apiClient.authorizedRequest<List<Statement>>(
                HttpMethod.Get,
                "/v1/categories/${category.id}/statements",
            )
        }
        val remoteState = runCatching {
            apiClient.authorizedRequest<UserState>(HttpMethod.Get, "/v1/user/state")
        }.getOrNull()

        localStore.clearAllUserData()
        remoteCategories.forEach { localStore.upsertCategory(it) }
        remoteStatements.forEach { localStore.upsertStatement(it) }
        if (remoteState != null) {
            localStore.upsertUserState(remoteState)
        }
    }

    private suspend fun upsertCategoryRemote(category: Category) {
        val created = runCatching {
            apiClient.authorizedRequest<Category>(
                HttpMethod.Post,
                "/v1/categories",
                CategoryCreateRequest(
                    id = category.id,
                    label = category.label,
                    created = category.created,
                    aiUse = category.aiUse,
                ),
            )
        }.getOrNull()
        if (created != null) {
            localStore.upsertCategory(created)
            return
        }

        val patched = apiClient.authorizedRequest<Category>(
            HttpMethod.Patch,
            "/v1/categories/${category.id}",
            CategoryUpdateRequest(
                label = category.label,
                aiUse = category.aiUse,
            ),
        )
        localStore.upsertCategory(patched)
    }

    private suspend fun upsertStatementRemote(statement: Statement) {
        val created = runCatching {
            apiClient.authorizedRequest<Statement>(
                HttpMethod.Post,
                "/v1/statements",
                StatementCreateRequest(
                    id = statement.id,
                    categoryId = statement.categoryId,
                    text = statement.text,
                    created = statement.created,
                ),
            )
        }.getOrNull()
        if (created != null) {
            localStore.upsertStatement(created)
            return
        }

        val patched = apiClient.authorizedRequest<Statement>(
            HttpMethod.Patch,
            "/v1/statements/${statement.id}",
            StatementUpdateRequest(text = statement.text),
        )
        localStore.upsertStatement(patched)
    }
}
