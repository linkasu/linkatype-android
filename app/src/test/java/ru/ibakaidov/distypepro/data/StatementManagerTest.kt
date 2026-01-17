package ru.ibakaidov.distypepro.data

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.repository.StatementsRepository
import ru.ibakaidov.distypepro.utils.Callback

class StatementManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getList_returnsSortedMap() = runTest {
        val repo = FakeStatementsRepository(
            list = listOf(
                Statement(id = "s1", categoryId = "c1", text = "First", created = 5),
                Statement(id = "s2", categoryId = "c1", text = "Second", created = 9),
            )
        )
        val manager = StatementManager(context, "c1", repo, testDispatcher, testDispatcher)

        var result: Map<String, String>? = null
        manager.getList(object : Callback<Map<String, String>> {
            override fun onDone(res: Map<String, String>) {
                result = res
            }
        })

        advanceUntilIdle()

        assertEquals(listOf("s2", "s1"), result?.keys?.toList())
        assertEquals("Second", result?.get("s2"))
    }

    @Test
    fun create_callsRepository() = runTest {
        val repo = FakeStatementsRepository()
        val manager = StatementManager(context, "cat", repo, testDispatcher, testDispatcher)

        var done = false
        manager.create("Hello", object : Callback<Unit> {
            override fun onDone(result: Unit) {
                done = true
            }
        })

        advanceUntilIdle()

        assertTrue(done)
        assertEquals("Hello", repo.createdText)
        assertEquals("cat", repo.createdCategoryId)
    }

    @Test
    fun edit_callsRepository() = runTest {
        val repo = FakeStatementsRepository()
        val manager = StatementManager(context, "cat", repo, testDispatcher, testDispatcher)

        var done = false
        manager.edit("id1", "Updated", object : Callback<Unit> {
            override fun onDone(result: Unit) {
                done = true
            }
        })

        advanceUntilIdle()

        assertTrue(done)
        assertEquals("id1", repo.updatedId)
        assertEquals("Updated", repo.updatedText)
    }

    @Test
    fun remove_callsRepository() = runTest {
        val repo = FakeStatementsRepository()
        val manager = StatementManager(context, "cat", repo, testDispatcher, testDispatcher)

        var done = false
        manager.remove("id2", object : Callback<Unit> {
            override fun onDone(result: Unit) {
                done = true
            }
        })

        advanceUntilIdle()

        assertTrue(done)
        assertEquals("id2", repo.deletedId)
    }

    private class FakeStatementsRepository(
        private val list: List<Statement> = emptyList(),
    ) : StatementsRepository {
        var createdCategoryId: String? = null
        var createdText: String? = null
        var updatedId: String? = null
        var updatedText: String? = null
        var deletedId: String? = null

        override suspend fun listByCategory(categoryId: String): List<Statement> = list

        override suspend fun create(categoryId: String, text: String, created: Long?): Statement {
            createdCategoryId = categoryId
            createdText = text
            return Statement(id = "temp", categoryId = categoryId, text = text, created = created ?: 0L)
        }

        override suspend fun update(id: String, text: String): Statement {
            updatedId = id
            updatedText = text
            return Statement(id = id, categoryId = "", text = text, created = 0L)
        }

        override suspend fun delete(id: String) {
            deletedId = id
        }
    }
}
