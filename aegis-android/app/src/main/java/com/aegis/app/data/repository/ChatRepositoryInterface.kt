package com.aegis.app.data.repository

import com.aegis.app.data.model.ChatMessage
import com.aegis.app.data.model.ChatResponse
import com.aegis.app.data.model.Conversation

/**
 * Contract for chat data access.
 * Allows ViewModels to be tested with a fake without requiring a real network.
 */
interface ChatRepositoryInterface {
    suspend fun createConversation(title: String = "New Conversation"): Conversation
    suspend fun getConversationHistory(conversationId: String): List<ChatMessage>
    suspend fun sendMessage(conversationId: String, content: String): ChatResponse
    suspend fun registerDeviceToken(userId: String, token: String)
    suspend fun clearHistory()
}
