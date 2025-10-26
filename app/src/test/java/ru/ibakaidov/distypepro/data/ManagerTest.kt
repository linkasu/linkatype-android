package ru.ibakaidov.distypepro.data

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.ibakaidov.distypepro.utils.Callback

class ManagerTest {

  private lateinit var testManager: TestManager
  private lateinit var mockDbRef: DatabaseReference

  @Before
  fun setUp() {
    mockDbRef = mockk(relaxed = true)
    testManager = TestManager(mockDbRef)
  }

  @Test
  fun remove_callsCorrectDatabasePath() {
    val key = "testKey123"
    val callbackSlot = slot<DatabaseReference.CompletionListener>()

    every {
      mockDbRef.child(key).removeValue(capture(callbackSlot))
    } returns mockk()

    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}
    }

    testManager.remove(key, callback)

    verify { mockDbRef.child(key).removeValue(any()) }
  }

  @Test
  fun remove_onSuccess_callsOnDone() {
    val key = "testKey"
    val callbackSlot = slot<DatabaseReference.CompletionListener>()
    var doneCalled = false

    every {
      mockDbRef.child(key).removeValue(capture(callbackSlot))
    } answers {
      callbackSlot.captured.onComplete(null, mockk())
      mockk()
    }

    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {
        doneCalled = true
      }
    }

    testManager.remove(key, callback)

    assertTrue(doneCalled)
  }

  @Test
  fun remove_onError_callsOnError() {
    val key = "testKey"
    val callbackSlot = slot<DatabaseReference.CompletionListener>()
    var errorCalled = false
    var receivedException: Exception? = null

    val dbError = mockk<DatabaseError>()
    val testException = Exception("Database error")
    every { dbError.toException() } returns testException

    every {
      mockDbRef.child(key).removeValue(capture(callbackSlot))
    } answers {
      callbackSlot.captured.onComplete(dbError, mockk())
      mockk()
    }

    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}

      override fun onError(exception: Exception?) {
        errorCalled = true
        receivedException = exception
      }
    }

    testManager.remove(key, callback)

    assertTrue(errorCalled)
    assertEquals(testException, receivedException)
  }

  @Test
  fun remove_withDifferentKeys_callsCorrectChild() {
    val keys = listOf("key1", "key2", "key3")

    keys.forEach { key ->
      val callback = object : Callback<Unit> {
        override fun onDone(result: Unit) {}
      }

      testManager.remove(key, callback)
      verify { mockDbRef.child(key).removeValue(any()) }
    }
  }

  @Test
  fun abstractMethods_implementedCorrectly() {
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {}
    }

    testManager.getList(callback)
    assertTrue(testManager.getListCalled)

    testManager.edit("key", "value", mockk())
    assertTrue(testManager.editCalled)

    testManager.create("value", mockk())
    assertTrue(testManager.createCalled)

    val root = testManager.getRoot()
    assertNotNull(root)
  }

  private class TestManager(private val dbRef: DatabaseReference) : Manager<String>() {
    var getListCalled = false
    var editCalled = false
    var createCalled = false

    override fun getList(callback: Callback<Map<String, String>>) {
      getListCalled = true
      callback.onDone(emptyMap())
    }

    override fun getRoot(): DatabaseReference = dbRef

    override fun edit(key: String, value: String, callback: Callback<Unit>) {
      editCalled = true
      callback.onDone(Unit)
    }

    override fun create(value: String, callback: Callback<Unit>) {
      createCalled = true
      callback.onDone(Unit)
    }
  }

  @Test
  fun remove_multipleCallbacks_allInvoked() {
    val key = "testKey"
    val callbackSlot = slot<DatabaseReference.CompletionListener>()
    val results = mutableListOf<String>()

    every {
      mockDbRef.child(key).removeValue(capture(callbackSlot))
    } answers {
      callbackSlot.captured.onComplete(null, mockk())
      mockk()
    }

    repeat(3) { index ->
      val callback = object : Callback<Unit> {
        override fun onDone(result: Unit) {
          results.add("callback_$index")
        }
      }
      testManager.remove(key, callback)
    }

    assertEquals(3, results.size)
  }
}

