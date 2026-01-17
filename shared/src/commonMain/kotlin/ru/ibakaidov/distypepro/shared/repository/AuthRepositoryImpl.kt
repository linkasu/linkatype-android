package ru.ibakaidov.distypepro.shared.repository

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.api.ApiException
import ru.ibakaidov.distypepro.shared.model.AuthResponse

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val tokenStorage: ru.ibakaidov.distypepro.shared.auth.TokenStorage,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AuthResponse {
        val response = apiClient.postRaw("/v1/auth", mapOf("email" to email, "password" to password))
        return handleAuthResponse(response)
    }

    override suspend fun register(email: String, password: String): AuthResponse {
        val response = apiClient.postRaw("/v1/auth/register", mapOf("email" to email, "password" to password))
        return handleAuthResponse(response)
    }

    override suspend fun resetPassword(email: String) {
        apiClient.post<Unit>("/v1/auth/reset", mapOf("email" to email))
    }

    override suspend fun refresh(): AuthResponse {
        val refreshed = apiClient.refreshToken()
        if (refreshed == null) {
            throw ApiException(401, null, "Unable to refresh")
        }
        val token = tokenStorage.getAccessToken().orEmpty()
        val userMap = refreshed.user
        val user = if (userMap != null) {
            ru.ibakaidov.distypepro.shared.model.User(
                id = userMap["id"].orEmpty(),
                email = userMap["email"].orEmpty(),
            )
        } else {
            tokenStorageUser()
        }
        return AuthResponse(token = token, user = user)
    }

    override suspend fun logout() {
        val refreshToken = tokenStorage.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            apiClient.postRaw(
                "/v1/auth/logout",
                headers = mapOf(HttpHeaders.Cookie to "refresh_token=$refreshToken"),
            )
        }
        tokenStorage.clear()
    }

    private suspend fun handleAuthResponse(response: HttpResponse): AuthResponse {
        val payload = apiClient.handleResponse<AuthResponse>(response)
        val refreshToken = extractRefreshToken(response)
        tokenStorage.setAccessToken(payload.token)
        if (!refreshToken.isNullOrBlank()) {
            tokenStorage.setRefreshToken(refreshToken)
        }
        return payload
    }

    private fun extractRefreshToken(response: HttpResponse): String? {
        val rawCookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        val cookie = rawCookies.firstOrNull { it.startsWith("refresh_token=") } ?: return null
        return cookie.substringAfter("refresh_token=").substringBefore(';')
    }

    private fun tokenStorageUser(): ru.ibakaidov.distypepro.shared.model.User {
        // Placeholder for refresh-only flows; user must be rehydrated from app storage if needed.
        return ru.ibakaidov.distypepro.shared.model.User(id = "", email = "")
    }
}
