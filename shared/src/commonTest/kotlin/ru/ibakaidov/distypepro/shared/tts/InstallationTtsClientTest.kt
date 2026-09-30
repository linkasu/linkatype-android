package ru.ibakaidov.distypepro.shared.tts

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstallationTtsClientTest {
    @Test
    fun synthesize_whenDisabled_keepsDirectPathWithoutRequests() = runTest {
        var requests = 0
        val client = client { requests += 1; error("disabled client must not request") }

        val result = InstallationTtsClient(
            enabled = false,
            storage = FakeStorage(),
            client = client,
        ).synthesize("text", "zahar")

        assertTrue(result.useDirect)
        assertEquals(0, requests)
    }

    @Test
    fun synthesize_withValidToken_usesStoredTokenWithoutBootstrap() = runTest {
        val storage = FakeStorage(storedToken = "stored", storedExpiresAtMillis = NOW + TWO_DAYS)
        val client = client { request ->
            assertEquals("/v1/tts/anonymous", request.url.encodedPath)
            assertEquals("stored", request.headers["X-TTS-Installation-Token"])
            assertEquals(null, request.headers[HttpHeaders.Authorization])
            respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK)
        }

        val result = InstallationTtsClient(true, storage, client = client, nowMillis = { NOW }).synthesize("text", "zahar")

        assertContentEquals(byteArrayOf(1, 2, 3), result.bytes)
    }

    @Test
    fun synthesize_onUnauthorized_refreshesOnceWithSameIdempotencyKey() = runTest {
        val storage = FakeStorage(storedToken = "old", storedExpiresAtMillis = NOW + TWO_DAYS)
        val keys = mutableListOf<String?>()
        var requestNumber = 0
        val client = client { request ->
            when (requestNumber++) {
                0 -> {
                    assertEquals("/v1/tts/anonymous", request.url.encodedPath)
                    assertEquals("old", request.headers["X-TTS-Installation-Token"])
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    keys += request.headers["Idempotency-Key"]
                    respond("", HttpStatusCode.Unauthorized)
                }
                1 -> {
                    assertEquals("/v1/tts/installations", request.url.encodedPath)
                    respond("{\"token\":\"new\",\"expiresAt\":${NOW + TWO_DAYS}}", HttpStatusCode.Created, jsonHeaders)
                }
                else -> {
                    assertEquals("/v1/tts/anonymous", request.url.encodedPath)
                    assertEquals("new", request.headers["X-TTS-Installation-Token"])
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    keys += request.headers["Idempotency-Key"]
                    respond(byteArrayOf(4), HttpStatusCode.OK)
                }
            }
        }

        val result = InstallationTtsClient(true, storage, client = client, nowMillis = { NOW }, newId = { "call-id" })
            .synthesize("text", "zahar")

        assertContentEquals(byteArrayOf(4), result.bytes)
        assertEquals(listOf<String?>("call-id", "call-id"), keys)
        assertEquals("new", storage.storedToken)
    }

    @Test
    fun synthesize_whenInstallationEndpointIsUnsupported_usesDirectPath() = runTest {
        val result = InstallationTtsClient(
            enabled = true,
            storage = FakeStorage(),
            client = client { respond("", HttpStatusCode.NotImplemented) },
            nowMillis = { NOW },
        ).synthesize("text", "zahar")

        assertTrue(result.useDirect)
        assertFalse(result.rateLimited)
    }

    @Test
    fun synthesize_whenSecondUnauthorized_doesNotUseDirectPath() = runTest {
        val storage = FakeStorage(storedToken = "old", storedExpiresAtMillis = NOW + TWO_DAYS)
        var requestNumber = 0
        val client = client { request ->
            when (requestNumber++) {
                0, 2 -> respond("", HttpStatusCode.Unauthorized)
                else -> {
                    assertEquals("/v1/tts/installations", request.url.encodedPath)
                    respond("{\"token\":\"new\",\"expiresAt\":${NOW + TWO_DAYS}}", HttpStatusCode.OK, jsonHeaders)
                }
            }
        }

        val result = InstallationTtsClient(true, storage, client = client, nowMillis = { NOW }).synthesize("text", "zahar")

        assertFalse(result.useDirect)
        assertFalse(result.rateLimited)
        assertEquals(3, requestNumber)
    }

    @Test
    fun synthesize_whenRateLimited_exposesRateLimitWithoutDirectFallback() = runTest {
        val client = client { request ->
            assertEquals("/v1/tts/anonymous", request.url.encodedPath)
            respond("", HttpStatusCode.TooManyRequests)
        }

        val result = InstallationTtsClient(
            enabled = true,
            storage = FakeStorage(storedToken = "token", storedExpiresAtMillis = NOW + TWO_DAYS),
            client = client,
            nowMillis = { NOW },
        ).synthesize("text", "zahar")

        assertTrue(result.rateLimited)
        assertFalse(result.useDirect)
    }

    private fun client(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json)
            }
        }

    private class FakeStorage(
        var storedToken: String? = null,
        var storedExpiresAtMillis: Long? = null,
    ) : TtsInstallationStorage {
        override fun getToken(): String? = storedToken

        override fun getExpiresAtMillis(): Long? = storedExpiresAtMillis

        override fun set(token: String?, expiresAtMillis: Long?) {
            storedToken = token
            storedExpiresAtMillis = expiresAtMillis
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val TWO_DAYS = 2L * 24L * 60L * 60L * 1_000L
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
