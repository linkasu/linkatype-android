package ru.ibakaidov.distypepro.screens.dialog

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.shared.SharedSdk
import ru.ibakaidov.distypepro.shared.api.ApiException
import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.shared.model.DialogMessage
import ru.ibakaidov.distypepro.shared.model.DialogRole
import javax.inject.Inject

@HiltViewModel
class DialogViewModel @Inject constructor(
    private val sdk: SharedSdk,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DialogUiState())
    val uiState: StateFlow<DialogUiState> = _uiState.asStateFlow()

    private val _events = Channel<DialogEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            val list = runCatching { sdk.dialogRepository.listChats() }.getOrElse { emptyList() }
            val sorted = sortChats(list)
            val chats = if (sorted.isEmpty()) {
                listOf(runCatching { sdk.dialogRepository.createChat(null) }.getOrNull() ?: return@launch)
            } else {
                sorted
            }
            val activeChatId = _uiState.value.activeChatId.takeIf { id -> chats.any { it.id == id } }
                ?: chats.firstOrNull()?.id

            _uiState.update { it.copy(chats = chats, activeChatId = activeChatId) }

            activeChatId?.let { loadMessages(it) }
        }
    }

    fun createChat() {
        viewModelScope.launch {
            val chat = runCatching { sdk.dialogRepository.createChat(null) }.getOrNull() ?: return@launch
            val chats = sortChats(listOf(chat) + _uiState.value.chats)
            _uiState.update { it.copy(chats = chats, activeChatId = chat.id) }
            loadMessages(chat.id)
            _events.send(DialogEvent.CloseDrawer)
        }
    }

    fun selectChat(chat: DialogChat) {
        _uiState.update { it.copy(activeChatId = chat.id) }
        loadMessages(chat.id)
        viewModelScope.launch {
            _events.send(DialogEvent.CloseDrawer)
        }
    }

    fun deleteChat(chat: DialogChat) {
        viewModelScope.launch {
            runCatching { sdk.dialogRepository.deleteChat(chat.id) }
            val chats = _uiState.value.chats.filterNot { it.id == chat.id }
            val activeChatId = if (_uiState.value.activeChatId == chat.id) {
                chats.firstOrNull()?.id
            } else {
                _uiState.value.activeChatId
            }
            if (chats.isEmpty()) {
                _uiState.update { it.copy(chats = emptyList(), activeChatId = null) }
                createChat()
            } else {
                _uiState.update { it.copy(chats = chats, activeChatId = activeChatId) }
                activeChatId?.let { loadMessages(it) }
            }
        }
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            val list = runCatching { sdk.dialogRepository.listMessages(chatId, MESSAGES_PAGE_SIZE, null) }
                .getOrElse { emptyList() }
            _uiState.update { it.copy(messages = list, suggestions = emptyList()) }
            _events.send(DialogEvent.ScrollToBottom)
        }
    }

    fun sendTextMessage(text: String) {
        val chatId = _uiState.value.activeChatId ?: return
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        viewModelScope.launch {
            _events.send(DialogEvent.ClearInput)
            val result = runCatching {
                sdk.dialogRepository.sendMessage(
                    chatId = chatId,
                    role = DialogRole.DISABLED_PERSON,
                    content = trimmedText,
                    source = "typed",
                    created = System.currentTimeMillis(),
                    includeSuggestions = true,
                )
            }.getOrNull()

            result?.let {
                addMessage(it.message)
                updateSuggestions(it.suggestions.orEmpty())
            }
        }
    }

    fun sendAudioMessage(audioBytes: ByteArray, filename: String) {
        val chatId = _uiState.value.activeChatId ?: return

        if (audioBytes.size <= WAV_HEADER_SIZE) {
            viewModelScope.launch {
                _events.send(DialogEvent.ShowError(DialogError.SendFailed))
            }
            return
        }
        if (audioBytes.size > MAX_AUDIO_BYTES) {
            viewModelScope.launch {
                _events.send(DialogEvent.ShowError(DialogError.AudioTooLarge))
            }
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                sdk.dialogRepository.sendAudioMessage(
                    chatId = chatId,
                    role = DialogRole.SPEAKER,
                    audioBytes = audioBytes,
                    mimeType = "audio/wav",
                    filename = filename,
                    created = System.currentTimeMillis(),
                    source = "audio",
                    includeSuggestions = true,
                )
            }

            val data = result.getOrNull()
            if (data == null) {
                val error = result.exceptionOrNull()
                Log.e(TAG, "sendAudioMessage failed", error)
                if (error is ApiException) {
                    Log.e(TAG, "sendAudioMessage status=${error.status} code=${error.error?.code} message=${error.error?.message}")
                }
                _events.send(DialogEvent.ShowError(DialogError.SendFailed))
                return@launch
            }

            val transcript = data.transcript
            val message = if (!transcript.isNullOrBlank() && data.message.content.isBlank()) {
                data.message.copy(content = transcript)
            } else {
                data.message
            }
            addMessage(message)
            updateSuggestions(data.suggestions.orEmpty())
        }
    }

    fun sendSuggestion(text: String) {
        val chatId = _uiState.value.activeChatId ?: return
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        clearSuggestions()

        viewModelScope.launch {
            val result = runCatching {
                sdk.dialogRepository.sendMessage(
                    chatId = chatId,
                    role = DialogRole.DISABLED_PERSON,
                    content = trimmedText,
                    source = "suggestion",
                    created = System.currentTimeMillis(),
                    includeSuggestions = true,
                )
            }.getOrNull()

            result?.let {
                addMessage(it.message)
                updateSuggestions(it.suggestions.orEmpty())
            }
        }
    }

    private fun updateSuggestions(suggestions: List<String>) {
        _uiState.update { it.copy(suggestions = suggestions.take(MAX_SUGGESTIONS)) }
    }

    private fun clearSuggestions() {
        _uiState.update { it.copy(suggestions = emptyList()) }
    }

    private fun addMessage(message: DialogMessage) {
        val messages = _uiState.value.messages + message
        val chats = _uiState.value.chats.map { chat ->
            if (chat.id == message.chatId) {
                chat.copy(
                    lastMessageAt = message.created,
                    messageCount = (chat.messageCount ?: 0) + 1,
                )
            } else {
                chat
            }
        }
        _uiState.update {
            it.copy(
                messages = messages,
                chats = sortChats(chats),
            )
        }
        viewModelScope.launch {
            _events.send(DialogEvent.ScrollToBottom)
        }
    }

    private fun sortChats(list: List<DialogChat>): List<DialogChat> =
        list.sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: it.created }

    companion object {
        private const val TAG = "DialogViewModel"
        private const val MESSAGES_PAGE_SIZE = 200
        private const val MAX_AUDIO_BYTES = 8 * 1024 * 1024
        private const val WAV_HEADER_SIZE = 44
        private const val MAX_SUGGESTIONS = 5
    }
}

data class DialogUiState(
    val chats: List<DialogChat> = emptyList(),
    val messages: List<DialogMessage> = emptyList(),
    val activeChatId: String? = null,
    val suggestions: List<String> = emptyList(),
) {
    val activeChat: DialogChat?
        get() = chats.firstOrNull { it.id == activeChatId }

    val isMessagesEmpty: Boolean
        get() = messages.isEmpty()
}

sealed class DialogEvent {
    data object ScrollToBottom : DialogEvent()
    data object CloseDrawer : DialogEvent()
    data object ClearInput : DialogEvent()
    data class ShowError(val error: DialogError) : DialogEvent()
}

enum class DialogError {
    SendFailed,
    AudioTooLarge,
}
