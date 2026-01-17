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
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.repository.CategoriesRepository
import ru.ibakaidov.distypepro.utils.Callback

class CategoryManagerTest {

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
        val repo = FakeCategoriesRepository(
            list = listOf(
                Category(id = "c1", label = "First", created = 10),
                Category(id = "c2", label = "Second", created = 20),
            )
        )
        val manager = CategoryManager(context, repo, testDispatcher, testDispatcher)

        var result: Map<String, String>? = null
        manager.getList(object : Callback<Map<String, String>> {
            override fun onDone(res: Map<String, String>) {
                result = res
            }
        })

        advanceUntilIdle()

        assertEquals(listOf("c2", "c1"), result?.keys?.toList())
        assertEquals("Second", result?.get("c2"))
    }

    @Test
    fun create_callsRepository() = runTest {
        val repo = FakeCategoriesRepository()
        val manager = CategoryManager(context, repo, testDispatcher, testDispatcher)

        var done = false
        manager.create("New", object : Callback<Unit> {
            override fun onDone(result: Unit) {
                done = true
            }
        })

        advanceUntilIdle()

        assertTrue(done)
        assertEquals("New", repo.createdLabel)
    }

    @Test
    fun edit_callsRepository() = runTest {
        val repo = FakeCategoriesRepository()
        val manager = CategoryManager(context, repo, testDispatcher, testDispatcher)

        var done = false
        manager.edit("id1", "Updated", object : Callback<Unit> {
            override fun onDone(result: Unit) {
                done = true
            }
        })

        advanceUntilIdle()

        assertTrue(done)
        assertEquals("id1", repo.updatedId)
        assertEquals("Updated", repo.updatedLabel)
    }

    @Test
    fun remove_callsRepository() = runTest {
        val repo = FakeCategoriesRepository()
        val manager = CategoryManager(context, repo, testDispatcher, testDispatcher)

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

    private class FakeCategoriesRepository(
        private val list: List<Category> = emptyList(),
    ) : CategoriesRepository {
        var createdLabel: String? = null
        var updatedId: String? = null
        var updatedLabel: String? = null
        var deletedId: String? = null

        override suspend fun list(): List<Category> = list

        override suspend fun create(label: String, created: Long?, aiUse: Boolean?): Category {
            createdLabel = label
            return Category(id = "temp", label = label, created = created ?: 0L)
        }

        override suspend fun update(id: String, label: String?, aiUse: Boolean?): Category {
            updatedId = id
            updatedLabel = label
            return Category(id = id, label = label ?: "", created = 0L)
        }

        override suspend fun delete(id: String) {
            deletedId = id
        }
    }
}
