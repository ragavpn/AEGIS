package com.aegis.app.data.repository

import com.aegis.app.data.model.ArticleInteraction
import com.aegis.app.data.model.UserPreferences

/**
 * Interface for user data operations (interactions + preferences).
 * Allows ViewModels to depend on an abstraction, enabling unit testing
 * without a live Supabase connection per AI_RULES.md §5.
 */
interface UserDataRepositoryInterface {
    suspend fun getInteraction(articleId: String): ArticleInteraction?
    suspend fun upsertInteraction(interaction: ArticleInteraction)
    suspend fun getPreferences(): UserPreferences?
    suspend fun upsertPreferences(prefs: UserPreferences)
}
