package ru.ibakaidov.distypepro.bank

import kotlin.test.Test
import kotlin.test.assertEquals

class SortUtilsTest {

    @Test
    fun `sort ascending sorts by value`() {
        val data = mapOf("b" to "Banana", "a" to "apple", "c" to "Cherry")

        val sorted = sortEntries(data, SortMode.ALPHABET_ASC)

        assertEquals(listOf("apple", "Banana", "Cherry"), sorted.values.toList())
    }

    @Test
    fun `sort descending sorts by value`() {
        val data = mapOf("b" to "Banana", "a" to "apple", "c" to "Cherry")

        val sorted = sortEntries(data, SortMode.ALPHABET_DESC)

        assertEquals(listOf("Cherry", "Banana", "apple"), sorted.values.toList())
    }
}
