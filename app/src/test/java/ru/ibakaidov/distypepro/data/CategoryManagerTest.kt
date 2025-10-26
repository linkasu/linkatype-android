package ru.ibakaidov.distypepro.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.utils.Callback

class CategoryManagerTest {

  private lateinit var categoryManager: CategoryManager
  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockRootRef: DatabaseReference
  private lateinit var mockCategoryRef: DatabaseReference

  @Before
  fun setUp() {
    mockkStatic(FirebaseDatabase::class)
    mockkStatic(FirebaseAuth::class)

    mockDatabase = mockk()
    mockAuth = mockk()
    mockUser = mockk()
    mockRootRef = mockk(relaxed = true)
    mockCategoryRef = mockk(relaxed = true)

    every { FirebaseDatabase.getInstance() } returns mockDatabase
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUserId123"
    every { mockDatabase.reference } returns mockRootRef
    every { mockRootRef.child(any()) } returns mockCategoryRef
    every { mockCategoryRef.child(any()) } returns mockCategoryRef

    categoryManager = CategoryManager()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun getRoot_returnsCorrectPath() {
    every { mockRootRef.child("users/testUserId123") } returns mockCategoryRef
    every { mockCategoryRef.child("Category") } returns mockCategoryRef

    val root = categoryManager.getRoot()

    verify { mockRootRef.child("users/testUserId123") }
    verify(atLeast = 1) { mockCategoryRef.child("Category") }
  }

  @Test
  fun getList_parsesDataCorrectly() {
    val listenerSlot = slot<ValueEventListener>()
    val mockSnapshot = mockk<DataSnapshot>()
    val mockChild1 = mockk<DataSnapshot>()
    val mockChild2 = mockk<DataSnapshot>()

    every { mockCategoryRef.orderByChild("created") } returns mockCategoryRef
    every { mockCategoryRef.addValueEventListener(capture(listenerSlot)) } returns mockk()

    val categoryData1 = mapOf(
      "id" to "cat1",
      "label" to "Category 1",
      "created" to 1000L
    )
    val categoryData2 = mapOf(
      "id" to "cat2",
      "label" to "Category 2",
      "created" to 2000L
    )

    every { mockChild1.value } returns categoryData1
    every { mockChild2.value } returns categoryData2
    every { mockSnapshot.children } returns listOf(mockChild1, mockChild2)

    var receivedResult: Map<String, String>? = null
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {
        receivedResult = result
      }
    }

    categoryManager.getList(callback)
    listenerSlot.captured.onDataChange(mockSnapshot)

    assertNotNull(receivedResult)
    assertEquals(2, receivedResult?.size)
    assertEquals("Category 2", receivedResult?.get("cat2"))
    assertEquals("Category 1", receivedResult?.get("cat1"))
  }

  @Test
  fun getList_onError_callsCallback() {
    val listenerSlot = slot<ValueEventListener>()
    val mockError = mockk<DatabaseError>()
    val testException = Exception("Database error")

    every { mockCategoryRef.orderByChild("created") } returns mockCategoryRef
    every { mockCategoryRef.addValueEventListener(capture(listenerSlot)) } returns mockk()
    every { mockError.toException() } returns testException

    var receivedError: Exception? = null
    val callback = object : Callback<Map<String, String>> {
      override fun onDone(result: Map<String, String>) {}

      override fun onError(exception: Exception?) {
        receivedError = exception
      }
    }

    categoryManager.getList(callback)
    listenerSlot.captured.onCancelled(mockError)

    assertEquals(testException, receivedError)
  }

  @Test
  fun edit_updatesLabel() {
    val taskSlot = slot<Task<Void>>()
    val mockTask = mockk<Task<Void>>(relaxed = true)
    val key = "cat123"
    val newLabel = "Updated Label"

    every { mockCategoryRef.updateChildren(any()) } returns mockTask
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

    categoryManager.edit(key, newLabel, callback)

    verify { mockCategoryRef.child(key) }
    verify { mockCategoryRef.updateChildren(mapOf("label" to newLabel)) }
    assertTrue(doneCalled)
  }

  @Test
  fun edit_onFailure_callsOnError() {
    val mockTask = mockk<Task<Void>>(relaxed = true)
    val testException = Exception("Update failed")

    every { mockCategoryRef.updateChildren(any()) } returns mockTask
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

    categoryManager.edit("key", "value", callback)

    assertEquals(testException, receivedError)
  }

  @Test
  fun create_generatesCorrectData() {
    val completionSlot = slot<DatabaseReference.CompletionListener>()
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)

    every { mockCategoryRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "generatedKey123"
    every { mockPushRef.updateChildren(any(), capture(completionSlot)) } answers {
      completionSlot.captured.onComplete(null, mockPushRef)
      Unit
    }

    val label = "New Category"
    var doneCalled = false
    val callback = object : Callback<Unit> {
      override fun onDone(result: Unit) {
        doneCalled = true
      }
    }

    categoryManager.create(label, callback)

    verify {
      mockPushRef.updateChildren(match { data ->
        data["label"] == label &&
          data["id"] == "generatedKey123" &&
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
    val testException = Exception("Create failed")

    every { mockCategoryRef.push() } returns mockPushRef
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

    categoryManager.create("label", callback)

    assertEquals(testException, receivedError)
  }

  @Test
  fun getList_sortsDescendingByCreated() {
    val listenerSlot = slot<ValueEventListener>()
    val mockSnapshot = mockk<DataSnapshot>()
    val mockChild1 = mockk<DataSnapshot>()
    val mockChild2 = mockk<DataSnapshot>()
    val mockChild3 = mockk<DataSnapshot>()

    every { mockCategoryRef.orderByChild("created") } returns mockCategoryRef
    every { mockCategoryRef.addValueEventListener(capture(listenerSlot)) } returns mockk()

    val data1 = mapOf("id" to "c1", "label" to "First", "created" to 1000L)
    val data2 = mapOf("id" to "c2", "label" to "Second", "created" to 3000L)
    val data3 = mapOf("id" to "c3", "label" to "Third", "created" to 2000L)

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

    categoryManager.getList(callback)
    listenerSlot.captured.onDataChange(mockSnapshot)

    val keys = receivedResult?.keys?.toList()
    assertEquals("c2", keys?.get(0))
    assertEquals("c3", keys?.get(1))
    assertEquals("c1", keys?.get(2))
  }
}

