package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.model.GlobalImportCategoryRequest
import ru.ibakaidov.distypepro.shared.model.GlobalCategory
import ru.ibakaidov.distypepro.shared.model.ImportStatus
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.session.AppMode
import ru.ibakaidov.distypepro.shared.session.InMemorySessionRepository
import ru.ibakaidov.distypepro.shared.session.SessionRepository

class GlobalRepositoryImpl(
    private val apiClient: ApiClient,
    private val sessionRepository: SessionRepository = InMemorySessionRepository(),
) : GlobalRepository {
    override suspend fun listCategories(includeStatements: Boolean): List<GlobalCategory> {
        ensureOnline(feature = "global_import_list")
        val query = if (includeStatements) "?include_statements=true" else ""
        return apiClient.authorizedRequest(HttpMethod.Get, "/v1/global/categories$query")
    }

    override suspend fun listStatements(categoryId: String): List<Statement> {
        ensureOnline(feature = "global_import_list_statements")
        val safeId = categoryId.encodeURLParameter()
        return apiClient.authorizedRequest(HttpMethod.Get, "/v1/global/categories/$safeId/statements")
    }

    override suspend fun importCategory(categoryId: String, force: Boolean): ImportStatus {
        ensureOnline(feature = "global_import")
        return apiClient.authorizedRequest(
            HttpMethod.Post,
            "/v1/global/import",
            GlobalImportCategoryRequest(categoryId = categoryId, force = force),
        )
    }

    private fun ensureOnline(feature: String) {
        if (sessionRepository.getMode() == AppMode.OFFLINE) {
            throw ModeRestrictedException(feature = feature)
        }
    }
}
