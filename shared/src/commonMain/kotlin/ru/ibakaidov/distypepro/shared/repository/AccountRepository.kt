package ru.ibakaidov.distypepro.shared.repository

interface AccountRepository {
    @Throws(Exception::class)
    suspend fun deleteAccount(deleteFirebase: Boolean = true)
}
