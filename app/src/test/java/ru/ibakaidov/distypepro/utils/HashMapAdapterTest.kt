package ru.ibakaidov.distypepro.utils

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.ibakaidov.distypepro.R

@RunWith(RobolectricTestRunner::class)
class HashMapAdapterTest {

  private lateinit var context: Context
  private lateinit var testData: Map<String, String>

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    testData = mapOf(
      "key1" to "value1",
      "key2" to "value2",
      "key3" to "value3"
    )
  }

  @Test
  fun getCount_returnsCorrectSize() {
    val adapter = HashMapAdapter(context, testData)

    assertEquals(3, adapter.count)
  }

  @Test
  fun getCount_withEmptyMap_returnsZero() {
    val adapter = HashMapAdapter(context, emptyMap())

    assertEquals(0, adapter.count)
  }

  @Test
  fun getItem_returnsCorrectValue() {
    val adapter = HashMapAdapter(context, testData)

    val keys = testData.keys.toList()
    keys.forEachIndexed { index, key ->
      assertEquals(testData[key], adapter.getItem(index))
    }
  }

  @Test
  fun getItem_withInvalidKey_returnsEmpty() {
    val dataWithNull = mapOf("key1" to "value1", "key2" to null)
    val adapter = HashMapAdapter(context, dataWithNull)

    assertEquals("", adapter.getItem(1))
  }

  @Test
  fun getItemId_returnsPosition() {
    val adapter = HashMapAdapter(context, testData)

    assertEquals(0L, adapter.getItemId(0))
    assertEquals(1L, adapter.getItemId(1))
    assertEquals(2L, adapter.getItemId(2))
  }

  @Test
  fun getKey_returnsCorrectKey() {
    val adapter = HashMapAdapter(context, testData)
    val keys = testData.keys.toList()

    keys.forEachIndexed { index, expectedKey ->
      assertEquals(expectedKey, adapter.getKey(index))
    }
  }

  @Test
  fun getEntry_returnsCorrectPair() {
    val adapter = HashMapAdapter(context, testData)
    val keys = testData.keys.toList()

    keys.forEachIndexed { index, key ->
      val entry = adapter.getEntry(index)
      assertEquals(key, entry.first)
      assertEquals(testData[key], entry.second)
    }
  }

  @Test
  fun getView_setsCorrectText() {
    val adapter = HashMapAdapter(context, testData)
    val parent = mockk<ViewGroup>(relaxed = true)

    val view = adapter.getView(0, null, parent)
    val titleView: TextView = view.findViewById(R.id.title)

    val keys = testData.keys.toList()
    assertEquals(testData[keys[0]], titleView.text)
  }

  @Test
  fun getView_reusesConvertView() {
    val adapter = HashMapAdapter(context, testData)
    val parent = mockk<ViewGroup>(relaxed = true)

    val firstView = adapter.getView(0, null, parent)
    val reusedView = adapter.getView(1, firstView, parent)

    assertEquals(firstView, reusedView)
  }

  @Test
  fun adapter_withSingleEntry_worksCorrectly() {
    val singleData = mapOf("singleKey" to "singleValue")
    val adapter = HashMapAdapter(context, singleData)

    assertEquals(1, adapter.count)
    assertEquals("singleValue", adapter.getItem(0))
    assertEquals("singleKey", adapter.getKey(0))
  }

  @Test
  fun adapter_maintainsKeyOrder() {
    val orderedData = linkedMapOf(
      "first" to "1",
      "second" to "2",
      "third" to "3"
    )
    val adapter = HashMapAdapter(context, orderedData)

    val keys = orderedData.keys.toList()
    assertEquals(keys[0], adapter.getKey(0))
    assertEquals(keys[1], adapter.getKey(1))
    assertEquals(keys[2], adapter.getKey(2))
  }
}

