package ru.ibakaidov.distypepro.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.ibakaidov.distypepro.utils.Callback

class StatementManagerTest {

  private lateinit var statementManager: StatementManager
  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockRootRef: DatabaseReference
  private lateinit var mockStatementRef: DatabaseReference
  private val testCategoryId = "category123"

  @Before
  fun setUp() {
    mockkStatic(FirebaseDatabase::class)
    mockkStatic(FirebaseAuth::class)

    mockDatabase = mockk()
    mockAuth = mockk()
    mockUser = mockk()
    mockRootRef = mockk(relaxed = true)
    mockStatementRef = mockk(relaxed = true)

    every { FirebaseDatabase.getInstance() } returns mockDatabase
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "userId456"
    every { mockDatabase.reference } returns mockRootRef
    every { mockRootRef.child(any()) } returns mockStatementRef
    every { mockStatementRef.child(any()) } returns mockStatementRef

    statementManager = StatementManager(testCategoryId)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun constructor_storesCategoryId() {
    val manager = StatementManager("catABC")
    assertNotNull(manager)
  }

  @Test
  fun getRoot_returnsCorrectPath() {
    every { mockRootRef.child("users/userId456") } returns mockStatementRef
    every { mockStatementRef.child("Category/$testCategoryId/statements") } returns mockStatementRef

    val root = statementManager.getRootForTest()

    verify { mockRootRef.child("users/userId456") }
    verify(atLeast = 1) { mockStatementRef.child(match { it.contains(testCategoryId) }) }
    assertNotNull(root)
  }

  @Test
  fun getList_parsesStatementsCorrectly() {
    val listenerSlot = slot<ValueEventListener>()
    val mockSnapshot = mockk<DataSnapshot>()
    val mockChild1 = mockk<DataSnapshot>()
    val mockChild2 = mockk<DataSnapshot>()

    every { mockStatementRef.orderByChild("created") } returns mockStatementRef
    every { mockStatementRef.addValueEventListener(capture(listenerSlot)) } returns mockk()

    val statement1 = mapOf(
      "id" to "stmt1",
      "categoryId" to testCategoryId,
      "text" to "Statement text 1",
      "created" to 1500L
    )
    val statement2 = mapOf(
      "id" to "stmt2",
      "categoryId" to testCategoryId,
      "text" to "Statement text 2",
      "created" to 2500L
    )

    every { mockChild1.value } returns statement1
    every { mockChild2.value } returns statement2
    every { mockSnapshot.children } returns listOf(mockChild1, mockChild2)

    var receivedResult: Map<String, String>? = null
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {
        receivedResult = result
      }
    }

    statementManager.getList(callback)
    listenerSlot.captured.onDataChange(mockSnapshot)

    assertNotNull(receivedResult)
    assertEquals(2, receivedResult?.size)
    assertEquals("Statement text 1", receivedResult?.get("stmt1"))
    assertEquals("Statement text 2", receivedResult?.get("stmt2"))
  }

  @Test
  fun getList_sortsDescendingByCreated() {
    val listenerSlot = slot<ValueEventListener>()
    val mockSnapshot = mockk<DataSnapshot>()
    val mockChild1 = mockk<DataSnapshot>()
    val mockChild2 = mockk<DataSnapshot>()
    val mockChild3 = mockk<DataSnapshot>()

    every { mockStatementRef.orderByChild("created") } returns mockStatementRef
    every { mockStatementRef.addValueEventListener(capture(listenerSlot)) } returns mockk()

    val data1 = mapOf("id" to "s1", "categoryId" to testCategoryId, "text" to "First", "created" to 1000L)
    val data2 = mapOf("id" to "s2", "categoryId" to testCategoryId, "text" to "Second", "created" to 3000L)
    val data3 = mapOf("id" to "s3", "categoryId" to testCategoryId, "text" to "Third", "created" to 2000L)

    every { mockChild1.value } returns data1
    every { mockChild2.value } returns data2
    every { mockChild3.value } returns data3
    every { mockSnapshot.children } returns listOf(mockChild1, mockChild2, mockChild3)

    var receivedResult: Map<String, String>? = null
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {
        receivedResult = result
      }
    }

    statementManager.getList(callback)
    listenerSlot.captured.onDataChange(mockSnapshot)

    val keys = receivedResult?.keys?.toList()
    assertEquals("s2", keys?.get(0))
    assertEquals("s3", keys?.get(1))
    assertEquals("s1", keys?.get(2))
  }

  @Test
  fun getList_onError_callsCallback() {
    val listenerSlot = slot<ValueEventListener>()
    val mockError = mockk<DatabaseError>()
    val testException = DatabaseException("Database error")

    every { mockStatementRef.orderByChild("created") } returns mockStatementRef
    every { mockStatementRef.addValueEventListener(capture(listenerSlot)) } returns mockk()
    every { mockError.toException() } returns testException

    var receivedError: Exception? = null
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {}

      override fun onError(exception: Exception?) {
        receivedError = exception
      }
    }

    statementManager.getList(callback)
    listenerSlot.captured.onCancelled(mockError)

    assertEquals(testException, receivedError)
  }

  @Test
  fun edit_updatesText() {
    val mockTask = mockk<Task<Void>>(relaxed = true)
    val key = "stmt123"
    val newText = "Updated text"

    every { mockStatementRef.updateChildren(any()) } returns mockTask
    every { mockTask.isSuccessful } returns true
    every { mockTask.addOnCompleteListener(any()) } answers {
      val listener = firstArg<com.google.android.gms.tasks.OnCompleteListener<Void>>()
      listener.onComplete(mockTask)
      mockTask
    }

    var doneCalled = false
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {
        doneCalled = true
      }
    }

    statementManager.edit(key, newText, callback)

    verify { mockStatementRef.child(key) }
    verify { mockStatementRef.updateChildren(mapOf("text" to newText)) }
    assertTrue(doneCalled)
  }

  @Test
  fun edit_onFailure_callsOnError() {
    val mockTask = mockk<Task<Void>>(relaxed = true)
    val testException = Exception("Update failed")

    every { mockStatementRef.updateChildren(any()) } returns mockTask
    every { mockTask.isSuccessful } returns false
    every { mockTask.exception } returns testException
    every { mockTask.addOnCompleteListener(any()) } answers {
      val listener = firstArg<com.google.android.gms.tasks.OnCompleteListener<Void>>()
      listener.onComplete(mockTask)
      mockTask
    }

    var receivedError: Exception? = null
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}

      override fun onError(exception: Exception?) {
        receivedError = exception
      }
    }

    statementManager.edit("key", "value", callback)

    assertEquals(testException, receivedError)
  }

  @Test
  fun create_generatesCorrectData() {
    val completionSlot = slot<DatabaseReference.CompletionListener>()
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)

    every { mockStatementRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "generatedStmtKey"
    every { mockPushRef.updateChildren(any(), capture(completionSlot)) } answers {
      completionSlot.captured.onComplete(null, mockPushRef)
      Unit
    }

    val text = "New statement"
    var doneCalled = false
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {
        doneCalled = true
      }
    }

    statementManager.create(text, callback)

    verify {
      mockPushRef.updateChildren(match { data ->
        data["text"] == text &&
          data["id"] == "generatedStmtKey" &&
          data["categoryId"] == testCategoryId &&
          data["created"] is Long
      }, any())
    }
    assertTrue(doneCalled)
  }

  @Test
  fun create_onError_callsOnError() {
    val completionSlot = slot<DatabaseReference.CompletionListener>()
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    val mockError = mockk<DatabaseError>()
    val testException = DatabaseException("Create failed")

    every { mockStatementRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "key"
    every { mockPushRef.updateChildren(any(), capture(completionSlot)) } answers {
      completionSlot.captured.onComplete(mockError, mockPushRef)
      Unit
    }
    every { mockError.toException() } returns testException

    var receivedError: Exception? = null
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}

      override fun onError(exception: Exception?) {
        receivedError = exception
      }
    }

    statementManager.create("text", callback)

    assertEquals(testException, receivedError)
  }

  @Test
  fun create_includesCategoryId() {
    val completionSlot = slot<DatabaseReference.CompletionListener>()
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)

    every { mockStatementRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "key"
    every { mockPushRef.updateChildren(any(), capture(completionSlot)) } answers {
      completionSlot.captured.onComplete(null, mockPushRef)
      Unit
    }

    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {}
    }

    statementManager.create("text", callback)

    verify {
      mockPushRef.updateChildren(match { data ->
        data["categoryId"] == testCategoryId
      }, any())
    }
  }

  @Test
  fun multipleCategoriesHaveSeparateManagers() {
    val manager1 = StatementManager("cat1")
    val manager2 = StatementManager("cat2")

    assertTrue(manager1 !== manager2)
  }

  private fun StatementManager.getRootForTest(): DatabaseReference {
    val method = StatementManager::class.java.getDeclaredMethod("getRoot")
    method.isAccessible = true
    return method.invoke(this) as DatabaseReference
  }
}
