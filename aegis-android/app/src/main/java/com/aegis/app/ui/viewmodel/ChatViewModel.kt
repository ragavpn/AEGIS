package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.model.ChatMessage
import com.aegis.app.data.repository.ChatRepository
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
    private val repository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentConversationId: String? = null

    init {
        initializeConversation()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            try {
                // For simplicity, create a new conversation per session.
                // In a full implementation, you'd load existing ones.
                val conv = repository.createConversation()
                currentConversationId = conv.id
            } catch (e: Exception) {
                // Fallback handled in sendMessage if ID is null
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        // Add user message
        val userMsg = ChatUiMessage(role = "user", content = content.trim())
        _messages.value = _messages.value + userMsg

        // Add loading placeholder
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

                // Replace loading placeholder with real response
                _messages.value = _messages.value.dropLast(1) +
                    ChatUiMessage(role = "assistant", content = response.content)
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1) +
                    ChatUiMessage(
                        role = "error",
                        content = "Failed to get response: ${e.message}"
                    )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
