package com.aegis.app.data.api

import com.aegis.app.data.model.ChatRequest
import com.aegis.app.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AegisApiService {

    @POST("conversations")
    suspend fun createConversation(@Body body: Map<String, String>): com.aegis.app.data.model.Conversation

    @retrofit2.http.GET("conversations/{id}")
    suspend fun getConversationHistory(@retrofit2.http.Path("id") id: String): com.aegis.app.data.model.ConversationHistoryResponse

    @DELETE("conversations")
    suspend fun deleteAllConversations(): Map<String, Any>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.aegis.app.data.model.MessageRequest
    ): com.aegis.app.data.model.ChatResponse

    @POST("notifications/register-token")
    suspend fun registerToken(@Body body: Map<String, String>): Map<String, Any>

    @retrofit2.http.PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@retrofit2.http.Path("id") id: String): Map<String, Any>

    @POST("podcasts/generate")
    suspend fun generatePodcast(@Body body: Map<String, String>): Map<String, String>

    @POST("podcasts/generate-daily")
    suspend fun generateDailyDigestPodcast(@Body body: Map<String, String>): Map<String, String>

    @retrofit2.http.GET("articles")
    suspend fun getArticles(): List<com.aegis.app.data.model.Article>

    @POST("articles/generate")
    suspend fun triggerGenerateArticles(): Map<String, Any>
}
