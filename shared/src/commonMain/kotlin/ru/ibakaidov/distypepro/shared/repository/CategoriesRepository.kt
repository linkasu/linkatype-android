package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.Category

interface CategoriesRepository {
    @Throws(Exception::class)
    suspend fun list(): List<Category>

    @Throws(Exception::class)
    suspend fun create(label: String, created: Long? = null, aiUse: Boolean? = null): Category

    @Throws(Exception::class)
    suspend fun update(id: String, label: String? = null, aiUse: Boolean? = null): Category

    @Throws(Exception::class)
    suspend fun delete(id: String)
}
