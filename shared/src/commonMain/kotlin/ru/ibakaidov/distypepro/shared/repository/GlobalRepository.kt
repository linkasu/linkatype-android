package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.GlobalCategory
import ru.ibakaidov.distypepro.shared.model.ImportStatus
import ru.ibakaidov.distypepro.shared.model.Statement

interface GlobalRepository {
    @Throws(Exception::class)
    suspend fun listCategories(includeStatements: Boolean = false): List<GlobalCategory>

    @Throws(Exception::class)
    suspend fun listStatements(categoryId: String): List<Statement>

    @Throws(Exception::class)
    suspend fun importCategory(categoryId: String, force: Boolean = false): ImportStatus
}
