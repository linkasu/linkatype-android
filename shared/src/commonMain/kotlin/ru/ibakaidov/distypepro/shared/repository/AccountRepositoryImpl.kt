package ru.ibakaidov.distypepro.shared.repository

import io.ktor.http.HttpMethod
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.session.AppMode
import ru.ibakaidov.distypepro.shared.session.InMemorySessionRepository
import ru.ibakaidov.distypepro.shared.session.SessionRepository

class AccountRepositoryImpl(
    private val apiClient: ApiClient,
    private val sessionRepository: SessionRepository = InMemorySessionRepository(),
) : AccountRepository {
    override suspend fun deleteAccount(deleteFirebase: Boolean) {
        if (sessionRepository.getMode() == AppMode.OFFLINE) {
            throw ModeRestrictedException(feature = "delete_account")
        }
        apiClient.authorizedRequest<Unit>(
            HttpMethod.Post,
            "/v1/user/delete",
            mapOf("delete_firebase" to deleteFirebase),
        )
    }
}
