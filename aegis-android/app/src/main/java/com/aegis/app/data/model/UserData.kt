package com.aegis.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticleInteraction(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("article_id") val articleId: String,
    val liked: Boolean? = null,
    val bookmarked: Boolean = false,
    @SerialName("read_duration_seconds") val readDurationSeconds: Int = 0,
    @SerialName("scroll_depth_percent") val scrollDepthPercent: Int = 0
)

@Serializable
data class UserPreferences(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val modules: List<String> = emptyList(),
    @SerialName("notification_flash") val notificationFlash: Boolean = true,
    @SerialName("notification_priority") val notificationPriority: Boolean = true,
    @SerialName("notification_routine") val notificationRoutine: Boolean = false,
    @SerialName("tts_speed") val ttsSpeed: Double = 1.0,
    @SerialName("dnd_enabled") val dndEnabled: Boolean = false
)

// All available intelligence modules
val ALL_MODULES = listOf(
    "GEOPOLITICS",
    "ENERGY",
    "FINANCE",
    "DEFENCE",
    "ECONOMICS",
    "TECHNOLOGY",
    "CLIMATE",
    "TRADE"
)
