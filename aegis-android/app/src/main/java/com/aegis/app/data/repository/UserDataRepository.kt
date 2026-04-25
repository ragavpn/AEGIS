package com.aegis.app.data.repository

import com.aegis.app.data.model.ArticleInteraction
import com.aegis.app.data.model.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId get() = supabase.auth.currentUserOrNull()?.id ?: error("Not logged in")

    // ── Article Interactions ─────────────────────────────────────────────────

    suspend fun getInteraction(articleId: String): ArticleInteraction? {
        return try {
            supabase.from("article_interactions")
                .select {
                    filter {
                        eq("article_id", articleId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<ArticleInteraction>()
        } catch (e: Exception) { null }
    }

    suspend fun upsertInteraction(interaction: ArticleInteraction) {
        supabase.from("article_interactions")
            .upsert(interaction.copy(userId = userId)) {
                onConflict = "user_id,article_id"
            }
    }

    // ── User Preferences ──────────────────────────────────────────────────────

    suspend fun getPreferences(): UserPreferences? {
        return try {
            supabase.from("user_preferences")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<UserPreferences>()
        } catch (e: Exception) { null }
    }

    suspend fun upsertPreferences(prefs: UserPreferences) {
        supabase.from("user_preferences")
            .upsert(prefs.copy(userId = userId)) {
                onConflict = "user_id"
            }
    }
}
