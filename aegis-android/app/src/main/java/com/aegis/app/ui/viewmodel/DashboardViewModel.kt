package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.model.Article
import com.aegis.app.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ArticleListState {
    object Loading : ArticleListState()
    data class Success(val articles: List<Article>) : ArticleListState()
    data class Error(val message: String) : ArticleListState()
}

sealed class ArticleDetailState {
    object Idle : ArticleDetailState()
    object Loading : ArticleDetailState()
    data class Success(val article: Article) : ArticleDetailState()
    data class Error(val message: String) : ArticleDetailState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<ArticleListState>(ArticleListState.Loading)
    val listState: StateFlow<ArticleListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Idle)
    val detailState: StateFlow<ArticleDetailState> = _detailState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _listState.value = ArticleListState.Loading
            try {
                val articles = repository.getArticles()
                _listState.value = ArticleListState.Success(articles)
            } catch (e: Exception) {
                _listState.value = ArticleListState.Error(e.message ?: "Failed to load articles")
            }
        }
    }

    /**
     * Pull-to-refresh handler: triggers the backend article generation pipeline first
     * (POST /articles/generate), then reloads the article list (GET /articles).
     * Silently ignores generate errors — the reload still runs so the user sees existing content.
     */
    fun triggerGenerateAndRefresh() {
        viewModelScope.launch {
            _listState.value = ArticleListState.Loading
            runCatching { repository.triggerGenerate() } // fire-and-forget; don't block on it
            try {
                val articles = repository.getArticles()
                _listState.value = ArticleListState.Success(articles)
            } catch (e: Exception) {
                _listState.value = ArticleListState.Error(e.message ?: "Failed to load articles")
            }
        }
    }

    fun loadArticle(id: String) {
        viewModelScope.launch {
            _detailState.value = ArticleDetailState.Loading
            try {
                val article = repository.getArticleById(id)
                _detailState.value = ArticleDetailState.Success(article)
            } catch (e: Exception) {
                _detailState.value = ArticleDetailState.Error(e.message ?: "Failed to load article")
            }
        }
    }
}
