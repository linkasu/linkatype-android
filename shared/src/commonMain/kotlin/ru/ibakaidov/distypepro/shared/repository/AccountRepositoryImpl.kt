package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import ru.ibakaidov.distypepro.shared.api.ApiClient

class AccountRepositoryImpl(
    private val apiClient: ApiClient,
) : AccountRepository {
    override suspend fun deleteAccount(deleteFirebase: Boolean) {
        apiClient.authorizedRequest<Unit>(
            HttpMethod.Post,
            "/v1/user/delete",
            mapOf("delete_firebase" to deleteFirebase),
        )
    }
}
