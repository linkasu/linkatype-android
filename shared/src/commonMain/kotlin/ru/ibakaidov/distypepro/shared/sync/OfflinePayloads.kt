package ru.ibakaidov.distypepro.shared.sync

import kotlinx.serialization.Serializable
import ru.ibakaidov.distypepro.shared.model.UserPreferences

@Serializable
data class OfflineCategoryPayload(
    val id: String,
    val label: String,
    val created: Long,
    val aiUse: Boolean = false,
)

@Serializable
data class OfflineCategoryUpdatePayload(
    val id: String,
    val label: String? = null,
    val aiUse: Boolean? = null,
)

@Serializable
data class OfflineStatementPayload(
    val id: String,
    val categoryId: String,
    val text: String,
    val created: Long,
)

@Serializable
data class OfflineStatementUpdatePayload(
    val id: String,
    val text: String,
)

@Serializable
data class OfflineQuickesPayload(
    val quickes: List<String>,
)

@Serializable
data class OfflineUserStatePayload(
    val inited: Boolean? = null,
    val quickes: List<String>? = null,
    val preferences: UserPreferences? = null,
)

@Serializable
data class OfflineDialogMessagePayload(
    val chatId: String,
    val role: String,
    val content: String,
    val source: String? = null,
    val created: Long,
    val includeSuggestions: Boolean? = null,
)
