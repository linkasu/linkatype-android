package ru.ibakaidov.distypepro.shared.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.auth.TokenStorage
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.model.ChangeEvent
import ru.ibakaidov.distypepro.shared.model.ChangesResponse
import ru.ibakaidov.distypepro.shared.testing.createTestDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class ChangesSyncerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `pollOnce does not overwrite newer local data`() = runTest {
        val localStore = LocalStore(createTestDatabase())
        localStore.upsertCategory(
            Category(
                id = "cat-1",
                label = "local",
                created = 1000,
                updatedAt = 2000,
            )
        )

        val incoming = Category(
            id = "cat-1",
            label = "remote",
            created = 1000,
            updatedAt = 1000,
        )
        val response = ChangesResponse(
            cursor = "cursor-1",
            changes = listOf(
                ChangeEvent(
                    entityType = "category",
                    entityId = "cat-1",
                    op = "upsert",
                    payload = json.encodeToJsonElement(incoming),
                    updatedAt = 1000,
                )
            ),
        )

        val engine = MockEngine {
            respond(
                content = json.encodeToString(response),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val syncer = ChangesSyncer(createApiClient(engine), localStore)

        syncer.pollOnce(limit = 100, timeoutSeconds = 1)

        val stored = localStore.findCategory("cat-1")
        assertEquals("local", stored?.label)
    }

    private fun createApiClient(engine: MockEngine): ApiClient {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return ApiClient("http://localhost", TestTokenStorage(), client)
    }

    private class TestTokenStorage : TokenStorage {
        private var accessToken: String? = "token"
        private var refreshToken: String? = "refresh"

        override fun getAccessToken(): String? = accessToken

        override fun setAccessToken(token: String?) {
            accessToken = token
        }

        override fun getRefreshToken(): String? = refreshToken

        override fun setRefreshToken(token: String?) {
            refreshToken = token
        }

        override fun clear() {
            accessToken = null
            refreshToken = null
        }
    }
}
