package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.model.ChatMessage
import com.aegis.app.data.repository.ChatRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiMessage(
    val role: String,   // "user" | "assistant" | "error"
    val content: String,
    val isLoading: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryInterface
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentConversationId: String? = null

    init {
        initializeConversation()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            try {
                val conv = repository.createConversation()
                currentConversationId = conv.id
            } catch (e: Exception) {
                // Fallback handled in sendMessage if ID is null
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMsg = ChatUiMessage(role = "user", content = content.trim())
        _messages.value = _messages.value + userMsg

        val loadingMsg = ChatUiMessage(role = "assistant", content = "...", isLoading = true)
        _messages.value = _messages.value + loadingMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                var convId = currentConversationId
                if (convId == null) {
                    val conv = repository.createConversation()
                    currentConversationId = conv.id
                    convId = conv.id
                }

                val response = repository.sendMessage(convId, content)

                _messages.value = _messages.value.dropLast(1) +
                    ChatUiMessage(role = "assistant", content = response.content)
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1) +
                    ChatUiMessage(
                        role = "error",
                        content = "Failed to get response. Check your connection."
                    )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Wipes the local UI list AND deletes all conversations on the backend. */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                repository.clearHistory()
                _messages.value = emptyList()
                currentConversationId = null
                initializeConversation()
            } catch (e: Exception) {
                _error.value = "Failed to clear history: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
