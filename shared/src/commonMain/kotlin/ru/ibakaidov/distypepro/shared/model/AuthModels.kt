package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User,
)
