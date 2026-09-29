package ru.ibakaidov.distypepro.shared.tts

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import ru.ibakaidov.distypepro.shared.api.createDefaultHttpClient
import ru.ibakaidov.distypepro.shared.session.SessionStorage
import ru.ibakaidov.distypepro.shared.utils.currentTimeMillis
import ru.ibakaidov.distypepro.shared.utils.generateId

class InstallationTtsClient(
    private val enabled: Boolean,
    private val storage: TtsInstallationStorage,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: HttpClient = createDefaultHttpClient(Json { ignoreUnknownKeys = true }),
    private val nowMillis: () -> Long = ::currentTimeMillis,
    private val newId: () -> String = ::generateId,
) {
    suspend fun synthesize(text: String, voice: String): InstallationTtsResponse {
        if (!enabled) return InstallationTtsResponse(useDirect = true)

        val idempotencyKey = newId()
        val token = token(forceRefresh = false)
        if (token.useDirect || token.rateLimited) {
            return InstallationTtsResponse(useDirect = token.useDirect, rateLimited = token.rateLimited)
        }
        return synthesize(text, voice, token.value ?: return InstallationTtsResponse(), idempotencyKey, allowRefresh = true)
    }

    private suspend fun synthesize(
        text: String,
        voice: String,
        token: String,
        idempotencyKey: String,
        allowRefresh: Boolean,
    ): InstallationTtsResponse {
        val response = try {
            client.post("$baseUrl/tts/anonymous") {
                contentType(ContentType.Application.Json)
                header(TOKEN_HEADER, token)
                header(IDEMPOTENCY_HEADER, idempotencyKey)
                setBody(buildJsonObject {
                    put("text", text)
                    put("voice", voice)
                })
            }
        } catch (_: Exception) {
            return InstallationTtsResponse()
        }

        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> InstallationTtsResponse(bytes = response.body())
            HttpStatusCode.Unauthorized if allowRefresh -> {
                val refreshed = token(forceRefresh = true)
                if (refreshed.useDirect || refreshed.rateLimited) {
                    InstallationTtsResponse(useDirect = refreshed.useDirect, rateLimited = refreshed.rateLimited)
                } else {
                    synthesize(
                        text,
                        voice,
                        refreshed.value ?: return InstallationTtsResponse(),
                        idempotencyKey,
                        allowRefresh = false,
                    )
                }
            }
            HttpStatusCode.NotFound, HttpStatusCode.NotImplemented -> InstallationTtsResponse(useDirect = true)
            HttpStatusCode.TooManyRequests -> InstallationTtsResponse(rateLimited = true)
            else -> InstallationTtsResponse()
        }
    }

    private suspend fun token(forceRefresh: Boolean): InstallationToken {
        val token = storage.getToken()
        val expiresAt = storage.getExpiresAtMillis()
        if (!forceRefresh && !token.isNullOrBlank() && expiresAt != null && expiresAt > nowMillis() + REFRESH_WINDOW_MILLIS) {
            return InstallationToken(value = token)
        }

        val response = try {
            client.post("$baseUrl/tts/installations") {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        } catch (_: Exception) {
            return InstallationToken()
        }
        if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NotImplemented) {
            return InstallationToken(useDirect = true)
        }
        if (response.status == HttpStatusCode.TooManyRequests) return InstallationToken(rateLimited = true)
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) return InstallationToken()

        val payload = runCatching { Json.decodeFromString<JsonObject>(response.body<ByteArray>().decodeToString()) }.getOrNull()
            ?: return InstallationToken()
        val newToken = payload["token"]?.jsonPrimitive?.contentOrNull ?: return InstallationToken()
        val expiresAtMillis = payload["expiresAt"]?.jsonPrimitive?.longOrNull
            ?: payload["expires_at"]?.jsonPrimitive?.longOrNull
            ?: return InstallationToken()
        val normalizedExpiresAt = if (expiresAtMillis < MILLIS_IN_SECOND_THRESHOLD) {
            expiresAtMillis * 1_000L
        } else {
            expiresAtMillis
        }
        storage.set(newToken, normalizedExpiresAt)
        return InstallationToken(value = newToken)
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://backend.linka.su/v1"
        private const val TOKEN_HEADER = "X-TTS-Installation-Token"
        private const val IDEMPOTENCY_HEADER = "Idempotency-Key"
        private const val REFRESH_WINDOW_MILLIS = 24L * 60L * 60L * 1_000L
        private const val MILLIS_IN_SECOND_THRESHOLD = 1_000_000_000_000L
    }

    private data class InstallationToken(
        val value: String? = null,
        val useDirect: Boolean = false,
        val rateLimited: Boolean = false,
    )
}

class SessionTtsInstallationStorage(private val sessionStorage: SessionStorage) : TtsInstallationStorage {
    override fun getToken(): String? = sessionStorage.getTtsInstallationToken()

    override fun getExpiresAtMillis(): Long? = sessionStorage.getTtsInstallationTokenExpiresAtMillis()

    override fun set(token: String?, expiresAtMillis: Long?) {
        sessionStorage.setTtsInstallationToken(token, expiresAtMillis)
    }
}

interface TtsInstallationStorage {
    fun getToken(): String?
    fun getExpiresAtMillis(): Long?
    fun set(token: String?, expiresAtMillis: Long?)
}

data class InstallationTtsResponse(
    val bytes: ByteArray? = null,
    val useDirect: Boolean = false,
    val rateLimited: Boolean = false,
)
