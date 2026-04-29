package com.aegis.app.data.repository

import com.aegis.app.data.model.Article

/**
 * Contract for article data access.
 * Allows ViewModels to be tested with a fake without requiring a real network or Supabase.
 */
interface ArticleRepositoryInterface {
    suspend fun getArticles(): List<Article>
    suspend fun triggerGenerate()
    suspend fun getArticleById(id: String): Article
}
