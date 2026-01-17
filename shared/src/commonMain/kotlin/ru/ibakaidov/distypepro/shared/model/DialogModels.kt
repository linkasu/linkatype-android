package ru.ibakaidov.distypepro.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DialogChat(
    val id: String,
    val title: String? = null,
    val created: Long,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
    @SerialName("last_message_at")
    val lastMessageAt: Long? = null,
    @SerialName("message_count")
    val messageCount: Int? = null,
)

@Serializable
data class DialogMessage(
    val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val source: String? = null,
    val created: Long,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
)

@Serializable
data class DialogMessageResult(
    val message: DialogMessage,
    val transcript: String? = null,
    val suggestions: List<String>? = null,
)

@Serializable
data class DialogSuggestion(
    val id: String,
    val chatId: String? = null,
    val messageId: String? = null,
    val text: String,
    val status: String,
    val categoryId: String? = null,
    val created: Long,
    @SerialName("updated_at")
    val updatedAt: Long? = null,
)

@Serializable
data class DialogSuggestionApplyItem(
    val id: String,
    val categoryId: String? = null,
    val categoryLabel: String? = null,
)

@Serializable
data class DialogSuggestionApplyResult(
    val created: List<DialogSuggestionCreated>,
    val applied: List<String>,
)

@Serializable
data class DialogSuggestionCreated(
    val categoryId: String,
    val statementId: String,
)
