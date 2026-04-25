package com.aegis.app.data.repository

import com.aegis.app.data.model.Article
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import com.aegis.app.data.api.AegisApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val apiService: AegisApiService
) {
    suspend fun getArticles(): List<Article> {
        return apiService.getArticles()
    }

    /** Calls POST /articles/generate to kick off the backend article pipeline. */
    suspend fun triggerGenerate() {
        apiService.triggerGenerateArticles()
    }

    suspend fun getArticleById(id: String): Article {
        return supabase
            .from("articles")
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeSingle<Article>()
    }
}
