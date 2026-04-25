package com.aegis.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val body: String,
    val type: String = "info",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("created_at") val createdAt: String
)
