package ru.ibakaidov.distypepro.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.auth.TokenStorage

class ApiClient(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    httpClient: HttpClient? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(this@ApiClient.json)
        }
    }

    internal suspend inline fun <reified T> get(path: String, token: String? = null): T {
        return request(HttpMethod.Get, path, token = token)
    }

    internal suspend inline fun <reified T> post(path: String, body: Any? = null, token: String? = null): T {
        return request(HttpMethod.Post, path, body, token)
    }

    internal suspend inline fun <reified T> put(path: String, body: Any? = null, token: String? = null): T {
        return request(HttpMethod.Put, path, body, token)
    }

    internal suspend inline fun <reified T> patch(path: String, body: Any? = null, token: String? = null): T {
        return request(HttpMethod.Patch, path, body, token)
    }

    internal suspend inline fun <reified T> delete(path: String, token: String? = null): T {
        return request(HttpMethod.Delete, path, token = token)
    }

    internal suspend inline fun <reified T> postMultipartAuthorized(
        path: String,
        payloadJson: String,
        audioBytes: ByteArray,
        filename: String,
        mimeType: String,
    ): T {
        val token = tokenStorage.getAccessToken()
        return try {
            postMultipart(path, payloadJson, audioBytes, filename, mimeType, token)
        } catch (ex: ApiException) {
            if (ex.status == 401 && refreshToken() != null) {
                val refreshed = tokenStorage.getAccessToken()
                postMultipart(path, payloadJson, audioBytes, filename, mimeType, refreshed)
            } else {
                throw ex
            }
        }
    }

    internal suspend inline fun <reified T> authorizedRequest(
        method: HttpMethod,
        path: String,
        body: Any? = null,
    ): T {
        val token = tokenStorage.getAccessToken()
        return try {
            request(method, path, body, token)
        } catch (ex: ApiException) {
            if (ex.status == 401 && refreshToken() != null) {
                val refreshed = tokenStorage.getAccessToken()
                request(method, path, body, refreshed)
            } else {
                throw ex
            }
        }
    }

    suspend fun refreshToken(): AuthRefreshResponse? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        val response = client.request(fullUrl("/v1/auth/refresh")) {
            method = HttpMethod.Post
            header(HttpHeaders.Cookie, "refresh_token=$refreshToken")
        }
        if (!response.status.isSuccess()) {
            tokenStorage.clear()
            return null
        }
        val payload = response.body<AuthRefreshResponse>()
        tokenStorage.setAccessToken(payload.token)
        return payload
    }

    suspend fun postRaw(path: String, body: Any? = null, headers: Map<String, String> = emptyMap()): HttpResponse {
        return client.request(fullUrl(path)) {
            method = HttpMethod.Post
            if (body != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }
            headers.forEach { (key, value) -> header(key, value) }
            buildDefaults(this)
        }
    }

    private suspend inline fun <reified T> postMultipart(
        path: String,
        payloadJson: String,
        audioBytes: ByteArray,
        filename: String,
        mimeType: String,
        token: String?,
    ): T {
        val response = client.request(fullUrl(path)) {
            method = HttpMethod.Post
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "payload",
                            payloadJson,
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            },
                        )
                        append(
                            "audio",
                            audioBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            },
                        )
                    },
                ),
            )
            buildDefaults(this)
        }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        token: String? = null,
    ): T {
        val response = client.request(fullUrl(path)) {
            this.method = method
            if (body != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            buildDefaults(this)
        }
        return handleResponse(response)
    }

    internal suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body()
        }
        val error = runCatching { response.body<ApiErrorResponse>() }.getOrNull()?.error
        throw ApiException(response.status.value, error)
    }

    private fun fullUrl(path: String): String = if (path.startsWith("http")) path else "$baseUrl$path"

    private fun buildDefaults(builder: HttpRequestBuilder) {
        builder.header(HttpHeaders.Accept, ContentType.Application.Json)
    }
}

@kotlinx.serialization.Serializable
data class AuthRefreshResponse(
    val token: String,
    val user: Map<String, String>? = null,
    val expiresAt: Long? = null,
)
