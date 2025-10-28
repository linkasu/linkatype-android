package ru.ibakaidov.distypepro.structures

data class Statement(
    val id: String,
    val categoryId: String,
    val text: String,
    val created: Long
) {
    companion object {
        fun fromMap(map: Map<*, *>): Statement {
            val created = (map["created"] as? Number)?.toLong() ?: 0L
            val id = map["id"] as? String ?: ""
            val categoryId = map["categoryId"] as? String ?: ""
            val text = map["text"] as? String ?: ""
            return Statement(id, categoryId, text, created)
        }
    }
}
