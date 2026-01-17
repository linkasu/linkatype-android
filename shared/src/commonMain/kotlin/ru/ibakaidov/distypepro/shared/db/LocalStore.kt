package ru.ibakaidov.distypepro.shared.db

import kotlinx.serialization.json.Json
import ru.ibakaidov.distypepro.shared.model.Category
import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.shared.model.DialogMessage
import ru.ibakaidov.distypepro.shared.model.DialogSuggestion
import ru.ibakaidov.distypepro.shared.model.OfflineQueueEntry
import ru.ibakaidov.distypepro.shared.model.Statement
import ru.ibakaidov.distypepro.shared.model.UserPreferences
import ru.ibakaidov.distypepro.shared.model.UserState

class LocalStore(private val database: LinkaDatabase) {
    private val queries = database.linkaDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    fun listCategories(): List<Category> =
        queries.selectAllCategories().executeAsList().map {
            Category(
                id = it.id,
                label = it.label,
                created = it.created,
                default = it.isDefault != 0L,
                aiUse = it.aiUse != 0L,
                updatedAt = it.updatedAt,
            )
        }

    fun findCategory(id: String): Category? =
        queries.selectCategoryById(id).executeAsOneOrNull()?.let {
            Category(
                id = it.id,
                label = it.label,
                created = it.created,
                default = it.isDefault != 0L,
                aiUse = it.aiUse != 0L,
                updatedAt = it.updatedAt,
            )
        }

    fun upsertCategory(category: Category) {
        queries.upsertCategory(
            id = category.id,
            label = category.label,
            created = category.created,
            is_default = if (category.default) 1 else 0,
            ai_use = if (category.aiUse) 1 else 0,
            updated_at = category.updatedAt,
        )
    }

    fun deleteCategory(id: String) {
        queries.deleteCategory(id)
        queries.deleteStatementsByCategory(id)
    }

    fun listStatements(categoryId: String): List<Statement> =
        queries.selectStatementsByCategory(categoryId).executeAsList().map {
            Statement(
                id = it.id,
                categoryId = it.categoryId,
                text = it.text,
                created = it.created,
                updatedAt = it.updatedAt,
            )
        }

    fun findStatement(id: String): Statement? =
        queries.selectStatementById(id).executeAsOneOrNull()?.let {
            Statement(
                id = it.id,
                categoryId = it.categoryId,
                text = it.text,
                created = it.created,
                updatedAt = it.updatedAt,
            )
        }

    fun upsertStatement(statement: Statement) {
        queries.upsertStatement(
            id = statement.id,
            category_id = statement.categoryId,
            text = statement.text,
            created = statement.created,
            updated_at = statement.updatedAt,
        )
    }

    fun deleteStatement(id: String) {
        queries.deleteStatement(id)
    }

    fun listQuickes(): List<String> =
        queries.selectQuickes().executeAsList().sortedBy { it.slot }.map { it.text }

    fun replaceQuickes(values: List<String>) {
        queries.transaction {
            queries.clearQuickes()
            values.forEachIndexed { index, text ->
                queries.replaceQuickes(slot = index.toLong(), text = text)
            }
        }
    }

    fun getUserState(): UserState? {
        val row = queries.getUserState("state").executeAsOneOrNull() ?: return null
        val preferences = row.preferences?.let { json.decodeFromString(UserPreferences.serializer(), it) }
        return UserState(
            inited = row.inited != 0L,
            quickes = listQuickes(),
            preferences = preferences,
        )
    }

    fun upsertUserState(state: UserState) {
        val preferences = state.preferences?.let { json.encodeToString(UserPreferences.serializer(), it) }
        queries.upsertUserState(
            id = "state",
            inited = if (state.inited) 1 else 0,
            preferences = preferences,
        )
        if (state.quickes.isNotEmpty()) {
            replaceQuickes(state.quickes)
        }
    }

    fun listChats(): List<DialogChat> =
        queries.selectDialogChats().executeAsList().map {
            DialogChat(
                id = it.id,
                title = it.title,
                created = it.created,
                updatedAt = it.updatedAt,
                lastMessageAt = it.lastMessageAt,
                messageCount = it.messageCount?.toInt(),
            )
        }

    fun upsertChat(chat: DialogChat) {
        queries.upsertDialogChat(
            id = chat.id,
            title = chat.title,
            created = chat.created,
            updated_at = chat.updatedAt,
            last_message_at = chat.lastMessageAt,
            message_count = chat.messageCount?.toLong(),
        )
    }

    fun deleteChat(id: String) {
        queries.deleteDialogChat(id)
        queries.clearDialogMessagesByChat(id)
    }

    fun listMessages(chatId: String, limit: Long): List<DialogMessage> =
        queries.selectDialogMessages(chatId, limit).executeAsList().map {
            DialogMessage(
                id = it.id,
                chatId = it.chatId,
                role = it.role,
                content = it.content,
                source = it.source,
                created = it.created,
                updatedAt = it.updatedAt,
            )
        }

    fun upsertMessage(message: DialogMessage) {
        queries.upsertDialogMessage(
            id = message.id,
            chat_id = message.chatId,
            role = message.role,
            content = message.content,
            source = message.source,
            created = message.created,
            updated_at = message.updatedAt,
        )
    }

    fun listSuggestions(status: String, limit: Long): List<DialogSuggestion> =
        queries.selectDialogSuggestions(status, limit).executeAsList().map {
            DialogSuggestion(
                id = it.id,
                chatId = it.chatId,
                messageId = it.messageId,
                text = it.text,
                status = it.status,
                categoryId = it.categoryId,
                created = it.created,
                updatedAt = it.updatedAt,
            )
        }

    fun upsertSuggestion(suggestion: DialogSuggestion) {
        queries.upsertDialogSuggestion(
            id = suggestion.id,
            chat_id = suggestion.chatId,
            message_id = suggestion.messageId,
            text = suggestion.text,
            status = suggestion.status,
            category_id = suggestion.categoryId,
            created = suggestion.created,
            updated_at = suggestion.updatedAt,
        )
    }

    fun removeSuggestion(id: String) {
        queries.removeDialogSuggestion(id)
    }

    fun enqueueOffline(entityType: String, opType: String, payload: String, createdAt: Long) {
        queries.enqueueOffline(
            entity_type = entityType,
            op_type = opType,
            payload = payload,
            created_at = createdAt,
            retry_count = 0,
            last_error = null,
        )
    }

    fun listOfflineQueue(): List<OfflineQueueEntry> =
        queries.selectOfflineQueue().executeAsList().map {
            OfflineQueueEntry(
                id = it.id,
                entityType = it.entityType,
                opType = it.opType,
                payload = it.payload,
                createdAt = it.createdAt,
                retryCount = it.retryCount,
                lastError = it.lastError,
            )
        }

    fun updateOfflineRetry(id: Long, retryCount: Long, lastError: String?) {
        queries.updateOfflineRetry(retry_count = retryCount, last_error = lastError, id = id)
    }

    fun deleteOfflineEntry(id: Long) {
        queries.deleteOfflineEntry(id)
    }

    fun getSyncValue(key: String): String? =
        queries.selectSyncValue(key).executeAsOneOrNull()?.value_

    fun setSyncValue(key: String, value: String?) {
        if (value == null) {
            queries.deleteSyncValue(key)
        } else {
            queries.upsertSyncValue(key, value)
        }
    }

    fun getSyncLong(key: String): Long? = getSyncValue(key)?.toLongOrNull()

    fun setSyncLong(key: String, value: Long?) {
        setSyncValue(key, value?.toString())
    }
}
