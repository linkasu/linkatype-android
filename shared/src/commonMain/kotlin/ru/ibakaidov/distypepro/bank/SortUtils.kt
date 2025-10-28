package ru.ibakaidov.distypepro.bank

import kotlin.collections.LinkedHashMap

fun sortEntries(data: Map<String, String>, mode: SortMode): Map<String, String> {
    val entries = data.entries.toList()
    val sorted = when (mode) {
        SortMode.ALPHABET_ASC -> entries.sortedBy { platformLowercase(it.value) }
        SortMode.ALPHABET_DESC -> entries.sortedByDescending { platformLowercase(it.value) }
    }
    val result = LinkedHashMap<String, String>(sorted.size)
    sorted.forEach { entry -> result[entry.key] = entry.value }
    return result
}

internal expect fun platformLowercase(value: String): String
