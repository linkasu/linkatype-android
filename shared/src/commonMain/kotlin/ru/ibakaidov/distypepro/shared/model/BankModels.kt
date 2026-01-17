package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val label: String,
    val created: Long,
    val default: Boolean = false,
    val aiUse: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
)

@Serializable
data class Statement(
    val id: String,
    val categoryId: String,
    val text: String,
    val created: Long,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
)

@Serializable
data class GlobalCategory(
    val id: String,
    val label: String,
    val created: Long,
    val default: Boolean? = null,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
    val statements: List<Statement>? = null,
)

@Serializable
data class ImportStatus(
    val status: String,
)
