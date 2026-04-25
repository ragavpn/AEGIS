package com.aegis.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val title: String,
    val body: String,
    val summary: String,
    val modules: List<String> = emptyList(),
    val sources: List<Source> = emptyList(),
    @SerialName("sweep_id") val sweepId: String? = null,
    @SerialName("graph_context_used") val graphContextUsed: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Source(
    val url: String? = null,
    val title: String? = null
)
