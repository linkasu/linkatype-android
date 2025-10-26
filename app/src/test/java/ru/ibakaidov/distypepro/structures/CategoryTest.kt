package ru.ibakaidov.distypepro.structures

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

  @Test
  fun fromMap_withValidData_returnsCategory() {
    val map = mapOf(
      "id" to "cat123",
      "label" to "Test Category",
      "created" to 1234567890L
    )

    val category = Category.fromMap(map)

    assertEquals("cat123", category.id)
    assertEquals("Test Category", category.label)
    assertEquals(1234567890L, category.created)
  }

  @Test
  fun fromMap_withNumberAsDouble_convertsToLong() {
    val map = mapOf(
      "id" to "cat456",
      "label" to "Another Category",
      "created" to 9876543210.0
    )

    val category = Category.fromMap(map)

    assertEquals("cat456", category.id)
    assertEquals("Another Category", category.label)
    assertEquals(9876543210L, category.created)
  }

  @Test
  fun fromMap_withMissingFields_returnsDefaults() {
    val map = emptyMap<String, Any>()

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun fromMap_withPartialData_usesDefaultsForMissing() {
    val map = mapOf("label" to "Partial Category")

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("Partial Category", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun fromMap_withNullValues_returnsDefaults() {
    val map = mapOf<String, Any?>(
      "id" to null,
      "label" to null,
      "created" to null
    )

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun fromMap_withWrongTypes_returnsDefaults() {
    val map = mapOf(
      "id" to 123,
      "label" to true,
      "created" to "not a number"
    )

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun toString_returnsLabel() {
    val category = Category("id1", "My Label", 12345L)

    assertEquals("My Label", category.toString())
  }

  @Test
  fun dataClass_equality_worksCorrectly() {
    val category1 = Category("id1", "Label", 1000L)
    val category2 = Category("id1", "Label", 1000L)
    val category3 = Category("id2", "Label", 1000L)

    assertEquals(category1, category2)
    assert(category1 != category3)
  }

  @Test
  fun dataClass_copy_worksCorrectly() {
    val original = Category("id1", "Original", 1000L)
    val copied = original.copy(label = "Modified")

    assertEquals("id1", copied.id)
    assertEquals("Modified", copied.label)
    assertEquals(1000L, copied.created)
  }
}

