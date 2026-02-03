package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryCreateRequest(
    val id: String,
    val label: String,
    val created: Long,
    @SerialName("aiUse") val aiUse: Boolean,
)

@Serializable
data class CategoryUpdateRequest(
    val label: String? = null,
    @SerialName("aiUse") val aiUse: Boolean? = null,
)

@Serializable
data class StatementCreateRequest(
    val id: String,
    val categoryId: String,
    val text: String,
    val created: Long,
)

@Serializable
data class StatementUpdateRequest(
    val text: String,
)

@Serializable
data class UserStateUpdateRequest(
    val inited: Boolean? = null,
    val quickes: List<String>? = null,
    val preferences: UserPreferences? = null,
)

@Serializable
data class GlobalImportCategoryRequest(
    @SerialName("category_id") val categoryId: String,
    val force: Boolean,
)
