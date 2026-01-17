package ru.ibakaidov.distypepro.shared.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.auth.TokenStorage
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.model.UserState
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor
import ru.ibakaidov.distypepro.shared.testing.createTestDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class UserStateRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `getState normalizes quickes to 6 slots`() = runTest {
        val state = UserState(inited = true, quickes = listOf("one", "two"))
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/user/state" -> respond(
                    content = json.encodeToString(state),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val repo = createRepository(engine)

        val result = repo.getState()

        assertEquals(6, result.quickes.size)
        assertEquals("one", result.quickes[0])
        assertEquals("two", result.quickes[1])
    }

    @Test
    fun `updateState enqueues offline entry on failure`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val localStore = LocalStore(createTestDatabase())
        val repo = UserStateRepositoryImpl(createApiClient(engine), localStore)

        val result = repo.updateState(inited = true, quickes = listOf("first"), preferences = null)

        assertEquals(6, result.quickes.size)
        val queued = localStore.listOfflineQueue().single()
        assertEquals(OfflineQueueProcessor.ENTITY_USER_STATE, queued.entityType)
        assertEquals(OfflineQueueProcessor.OP_UPDATE, queued.opType)
    }

    private fun createRepository(engine: MockEngine): UserStateRepositoryImpl {
        val localStore = LocalStore(createTestDatabase())
        return UserStateRepositoryImpl(createApiClient(engine), localStore)
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
