package ru.ibakaidov.distypepro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.structures.Statement

class EdgeCasesTest {

  @Test
  fun category_fromMap_emptyStrings() {
    val map = mapOf(
      "id" to "",
      "label" to "",
      "created" to 0L
    )

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun category_fromMap_veryLongStrings() {
    val longString = "a".repeat(10000)
    val map = mapOf(
      "id" to longString,
      "label" to longString,
      "created" to Long.MAX_VALUE
    )

    val category = Category.fromMap(map)

    assertEquals(longString, category.id)
    assertEquals(longString, category.label)
    assertEquals(Long.MAX_VALUE, category.created)
  }

  @Test
  fun category_fromMap_specialCharacters() {
    val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~абвгдёжз"
    val map = mapOf(
      "id" to specialChars,
      "label" to specialChars,
      "created" to 12345L
    )

    val category = Category.fromMap(map)

    assertEquals(specialChars, category.id)
    assertEquals(specialChars, category.label)
  }

  @Test
  fun category_fromMap_unicodeEmoji() {
    val emoji = "😀🎉🚀💯"
    val map = mapOf(
      "id" to "id1",
      "label" to emoji,
      "created" to 1000L
    )

    val category = Category.fromMap(map)

    assertEquals(emoji, category.label)
  }

  @Test
  fun statement_fromMap_emptyStrings() {
    val map = mapOf(
      "id" to "",
      "categoryId" to "",
      "text" to "",
      "created" to 0L
    )

    val statement = Statement.fromMap(map)

    assertEquals("", statement.id)
    assertEquals("", statement.categoryId)
    assertEquals("", statement.text)
    assertEquals(0L, statement.created)
  }

  @Test
  fun statement_fromMap_veryLongText() {
    val longText = "Lorem ipsum ".repeat(1000)
    val map = mapOf(
      "id" to "id",
      "categoryId" to "catId",
      "text" to longText,
      "created" to 1000L
    )

    val statement = Statement.fromMap(map)

    assertEquals(longText, statement.text)
  }

  @Test
  fun statement_fromMap_specialCharactersInText() {
    val specialText = "Текст с\nпереносами\tи\rспецсимволами"
    val map = mapOf(
      "id" to "id",
      "categoryId" to "cat",
      "text" to specialText,
      "created" to 500L
    )

    val statement = Statement.fromMap(map)

    assertEquals(specialText, statement.text)
  }

  @Test
  fun category_fromMap_negativeTimestamp() {
    val map = mapOf(
      "id" to "id",
      "label" to "label",
      "created" to -1000L
    )

    val category = Category.fromMap(map)

    assertEquals(-1000L, category.created)
  }

  @Test
  fun category_fromMap_zeroTimestamp() {
    val map = mapOf(
      "id" to "id",
      "label" to "label",
      "created" to 0L
    )

    val category = Category.fromMap(map)

    assertEquals(0L, category.created)
  }

  @Test
  fun category_fromMap_mixedCaseKeys() {
    val map = mapOf(
      "ID" to "id1",
      "Label" to "Label1",
      "Created" to 1000L
    )

    val category = Category.fromMap(map)

    assertEquals("", category.id)
    assertEquals("", category.label)
    assertEquals(0L, category.created)
  }

  @Test
  fun category_fromMap_extraFields() {
    val map = mapOf(
      "id" to "id1",
      "label" to "label1",
      "created" to 1000L,
      "extraField1" to "extra",
      "extraField2" to 999
    )

    val category = Category.fromMap(map)

    assertEquals("id1", category.id)
    assertEquals("label1", category.label)
    assertEquals(1000L, category.created)
  }

  @Test
  fun statement_fromMap_extraFields() {
    val map = mapOf(
      "id" to "stmt1",
      "categoryId" to "cat1",
      "text" to "text",
      "created" to 2000L,
      "unused" to "data"
    )

    val statement = Statement.fromMap(map)

    assertEquals("stmt1", statement.id)
    assertEquals("cat1", statement.categoryId)
    assertEquals("text", statement.text)
    assertEquals(2000L, statement.created)
  }

  @Test
  fun category_toString_emptyLabel() {
    val category = Category("id", "", 1000L)

    assertEquals("", category.toString())
  }

  @Test
  fun category_equals_differentCreated_notEqual() {
    val cat1 = Category("id", "label", 1000L)
    val cat2 = Category("id", "label", 2000L)

    assertNotEquals(cat1, cat2)
  }

  @Test
  fun statement_equals_differentText_notEqual() {
    val stmt1 = Statement("id", "cat", "text1", 1000L)
    val stmt2 = Statement("id", "cat", "text2", 1000L)

    assertNotEquals(stmt1, stmt2)
  }

  @Test
  fun category_copy_withNoChanges() {
    val original = Category("id", "label", 1000L)
    val copy = original.copy()

    assertEquals(original, copy)
  }

  @Test
  fun statement_copy_withNoChanges() {
    val original = Statement("id", "cat", "text", 1000L)
    val copy = original.copy()

    assertEquals(original, copy)
  }

  @Test
  fun category_fromMap_integerAsCreated() {
    val map = mapOf(
      "id" to "id",
      "label" to "label",
      "created" to 1000
    )

    val category = Category.fromMap(map)

    assertEquals(1000L, category.created)
  }

  @Test
  fun statement_fromMap_floatAsCreated() {
    val map = mapOf(
      "id" to "id",
      "categoryId" to "cat",
      "text" to "text",
      "created" to 1000.5f
    )

    val statement = Statement.fromMap(map)

    assertEquals(1000L, statement.created)
  }

  @Test
  fun category_hashCode_consistency() {
    val cat1 = Category("id", "label", 1000L)
    val cat2 = Category("id", "label", 1000L)

    assertEquals(cat1.hashCode(), cat2.hashCode())
  }

  @Test
  fun statement_hashCode_consistency() {
    val stmt1 = Statement("id", "cat", "text", 1000L)
    val stmt2 = Statement("id", "cat", "text", 1000L)

    assertEquals(stmt1.hashCode(), stmt2.hashCode())
  }

  @Test
  fun category_fromMap_whitespaceInValues() {
    val map = mapOf(
      "id" to "  id with spaces  ",
      "label" to "\tlabel with tab\n",
      "created" to 1000L
    )

    val category = Category.fromMap(map)

    assertEquals("  id with spaces  ", category.id)
    assertEquals("\tlabel with tab\n", category.label)
  }
}

