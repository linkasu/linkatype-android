package ru.ibakaidov.distypepro.structures

data class Category(
    val id: String,
    val label: String,
    val created: Long
) {
    override fun toString(): String = label

    companion object {
        fun fromMap(map: Map<*, *>): Category {
            val created = (map["created"] as? Number)?.toLong() ?: 0L
            val id = map["id"] as? String ?: ""
            val label = map["label"] as? String ?: ""
            return Category(id, label, created)
        }
    }
}
