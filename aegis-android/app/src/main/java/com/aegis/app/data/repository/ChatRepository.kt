package com.aegis.app.data.repository

import com.aegis.app.data.api.AegisApiService
import com.aegis.app.data.model.ChatMessage
import com.aegis.app.data.model.ChatRequest
import com.aegis.app.data.model.ChatResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: AegisApiService
) : ChatRepositoryInterface {
    override suspend fun createConversation(title: String): com.aegis.app.data.model.Conversation {
        return api.createConversation(mapOf("title" to title))
    }

    override suspend fun getConversationHistory(conversationId: String): List<ChatMessage> {
        return api.getConversationHistory(conversationId).messages
    }

    override suspend fun sendMessage(conversationId: String, content: String): ChatResponse {
        return api.sendMessage(conversationId, com.aegis.app.data.model.MessageRequest(content))
    }

    override suspend fun registerDeviceToken(userId: String, token: String) {
        api.registerToken(mapOf("userId" to userId, "token" to token))
    }

    override suspend fun clearHistory() {
        api.deleteAllConversations()
    }
}
