package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.AuthResponse

interface AuthRepository {
    @Throws(Exception::class)
    suspend fun login(email: String, password: String): AuthResponse

    @Throws(Exception::class)
    suspend fun register(email: String, password: String): AuthResponse

    @Throws(Exception::class)
    suspend fun resetPassword(email: String)

    @Throws(Exception::class)
    suspend fun refresh(): AuthResponse

    @Throws(Exception::class)
    suspend fun logout()
}
