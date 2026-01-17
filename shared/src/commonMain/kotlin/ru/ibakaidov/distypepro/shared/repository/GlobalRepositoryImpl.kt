package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.model.GlobalCategory
import ru.ibakaidov.distypepro.shared.model.ImportStatus
import ru.ibakaidov.distypepro.shared.model.Statement

class GlobalRepositoryImpl(
    private val apiClient: ApiClient,
) : GlobalRepository {
    override suspend fun listCategories(includeStatements: Boolean): List<GlobalCategory> {
        val query = if (includeStatements) "?include_statements=true" else ""
        return apiClient.authorizedRequest(HttpMethod.Get, "/v1/global/categories$query")
    }

    override suspend fun listStatements(categoryId: String): List<Statement> {
        val safeId = categoryId.encodeURLParameter()
        return apiClient.authorizedRequest(HttpMethod.Get, "/v1/global/categories/$safeId/statements")
    }

    override suspend fun importCategory(categoryId: String, force: Boolean): ImportStatus {
        return apiClient.authorizedRequest(
            HttpMethod.Post,
            "/v1/global/import",
            mapOf("category_id" to categoryId, "force" to force),
        )
    }
}
