package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.shared.model.DialogMessage
import ru.ibakaidov.distypepro.shared.model.DialogMessageResult
import ru.ibakaidov.distypepro.shared.model.DialogSuggestion
import ru.ibakaidov.distypepro.shared.model.DialogSuggestionApplyItem
import ru.ibakaidov.distypepro.shared.model.DialogSuggestionApplyResult

interface DialogRepository {
    @Throws(Exception::class)
    suspend fun listChats(): List<DialogChat>

    @Throws(Exception::class)
    suspend fun createChat(title: String? = null): DialogChat

    @Throws(Exception::class)
    suspend fun deleteChat(id: String)

    @Throws(Exception::class)
    suspend fun listMessages(chatId: String, limit: Int? = null, before: Long? = null): List<DialogMessage>

    @Throws(Exception::class)
    suspend fun sendMessage(
        chatId: String,
        role: String,
        content: String,
        source: String? = null,
        created: Long? = null,
        includeSuggestions: Boolean? = null,
    ): DialogMessageResult

    @Throws(Exception::class)
    suspend fun sendAudioMessage(
        chatId: String,
        role: String,
        audioBytes: ByteArray,
        mimeType: String,
        filename: String,
        created: Long? = null,
        source: String? = null,
        includeSuggestions: Boolean? = null,
    ): DialogMessageResult

    @Throws(Exception::class)
    suspend fun listSuggestions(status: String = "pending", limit: Int = 200): List<DialogSuggestion>

    @Throws(Exception::class)
    suspend fun applySuggestions(items: List<DialogSuggestionApplyItem>): DialogSuggestionApplyResult

    @Throws(Exception::class)
    suspend fun dismissSuggestions(ids: List<String>)
}
