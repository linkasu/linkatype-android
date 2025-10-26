package ru.ibakaidov.distypepro.structures

import org.junit.Assert.assertEquals
import org.junit.Test

class StatementTest {

  @Test
  fun fromMap_withValidData_returnsStatement() {
    val map = mapOf(
      "id" to "stmt123",
      "categoryId" to "cat456",
      "text" to "Test statement text",
      "created" to 1234567890L
    )

    val statement = Statement.fromMap(map)

    assertEquals("stmt123", statement.id)
    assertEquals("cat456", statement.categoryId)
    assertEquals("Test statement text", statement.text)
    assertEquals(1234567890L, statement.created)
  }

  @Test
  fun fromMap_withNumberAsDouble_convertsToLong() {
    val map = mapOf(
      "id" to "stmt789",
      "categoryId" to "cat123",
      "text" to "Another statement",
      "created" to 9876543210.0
    )

    val statement = Statement.fromMap(map)

    assertEquals("stmt789", statement.id)
    assertEquals("cat123", statement.categoryId)
    assertEquals("Another statement", statement.text)
    assertEquals(9876543210L, statement.created)
  }

  @Test
  fun fromMap_withMissingFields_returnsDefaults() {
    val map = emptyMap<String, Any>()

    val statement = Statement.fromMap(map)

    assertEquals("", statement.id)
    assertEquals("", statement.categoryId)
    assertEquals("", statement.text)
    assertEquals(0L, statement.created)
  }

  @Test
  fun fromMap_withPartialData_usesDefaultsForMissing() {
    val map = mapOf(
      "text" to "Partial statement",
      "categoryId" to "cat999"
    )

    val statement = Statement.fromMap(map)

    assertEquals("", statement.id)
    assertEquals("cat999", statement.categoryId)
    assertEquals("Partial statement", statement.text)
    assertEquals(0L, statement.created)
  }

  @Test
  fun fromMap_withNullValues_returnsDefaults() {
    val map = mapOf<String, Any?>(
      "id" to null,
      "categoryId" to null,
      "text" to null,
      "created" to null
    )

    val statement = Statement.fromMap(map)

    assertEquals("", statement.id)
    assertEquals("", statement.categoryId)
    assertEquals("", statement.text)
    assertEquals(0L, statement.created)
  }

  @Test
  fun fromMap_withWrongTypes_returnsDefaults() {
    val map = mapOf(
      "id" to 123,
      "categoryId" to true,
      "text" to 456.78,
      "created" to "not a number"
    )

    val statement = Statement.fromMap(map)

    assertEquals("", statement.id)
    assertEquals("", statement.categoryId)
    assertEquals("", statement.text)
    assertEquals(0L, statement.created)
  }

  @Test
  fun dataClass_equality_worksCorrectly() {
    val statement1 = Statement("id1", "cat1", "Text", 1000L)
    val statement2 = Statement("id1", "cat1", "Text", 1000L)
    val statement3 = Statement("id2", "cat1", "Text", 1000L)

    assertEquals(statement1, statement2)
    assert(statement1 != statement3)
  }

  @Test
  fun dataClass_copy_worksCorrectly() {
    val original = Statement("id1", "cat1", "Original text", 1000L)
    val copied = original.copy(text = "Modified text")

    assertEquals("id1", copied.id)
    assertEquals("cat1", copied.categoryId)
    assertEquals("Modified text", copied.text)
    assertEquals(1000L, copied.created)
  }
}

