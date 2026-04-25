package com.aegis.app.data.model

import com.google.gson.annotations.SerializedName

// ── Request ───────────────────────────────────────────────────────────────────
data class ChatRequest(
    val messages: List<ChatMessage>
)

data class ChatMessage(
    val role: String,   // "user" | "assistant"
    val content: String
)

// ── Response ──────────────────────────────────────────────────────────────────
data class ChatResponse(
    val role: String,
    val content: String,
    val meta: ChatMeta? = null
)

data class ChatMeta(
    @SerializedName("entitiesExtracted") val entitiesExtracted: List<String>? = null,
    @SerializedName("graphContextUsed") val graphContextUsed: Boolean = false
)

data class MessageRequest(
    val content: String
)

data class Conversation(
    val id: String,
    val title: String?,
    val created_at: String
)

data class ConversationHistoryResponse(
    val messages: List<ChatMessage>
)
