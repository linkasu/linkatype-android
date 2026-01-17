package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.Statement

interface StatementsRepository {
    @Throws(Exception::class)
    suspend fun listByCategory(categoryId: String): List<Statement>

    @Throws(Exception::class)
    suspend fun create(categoryId: String, text: String, created: Long? = null): Statement

    @Throws(Exception::class)
    suspend fun update(id: String, text: String): Statement

    @Throws(Exception::class)
    suspend fun delete(id: String)
}
