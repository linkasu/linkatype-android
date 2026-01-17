package ru.ibakaidov.distypepro.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val error: ApiError? = null,
)

class ApiException(
    val status: Int,
    val error: ApiError? = null,
    message: String? = error?.message,
) : RuntimeException(message ?: "Request failed")
