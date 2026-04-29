package com.aegis.app.ui.viewmodel

import com.aegis.app.data.model.Article
import com.aegis.app.data.repository.ArticleRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeArticleRepository : ArticleRepositoryInterface {
    var articlesToReturn = listOf<Article>()
    var shouldThrowError = false
    var triggerGenerateCalled = false

    override suspend fun getArticles(): List<Article> {
        if (shouldThrowError) throw Exception("Network error")
        return articlesToReturn
    }

    override suspend fun triggerGenerate() {
        triggerGenerateCalled = true
    }

    override suspend fun getArticleById(id: String): Article {
        if (shouldThrowError) throw Exception("Not found")
        return articlesToReturn.find { it.id == id } ?: throw Exception("Not found")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private lateinit var viewModel: DashboardViewModel
    private lateinit var fakeRepo: FakeArticleRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeArticleRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadArticles success sets state to Success with articles`() = runTest {
        val mockArticles = listOf(
            Article(id = "1", title = "A1", body = "B1", summary = "S1"),
            Article(id = "2", title = "A2", body = "B2", summary = "S2")
        )
        fakeRepo.articlesToReturn = mockArticles

        viewModel = DashboardViewModel(fakeRepo) // init triggers loadArticles
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.listState.value
        assertTrue(state is ArticleListState.Success)
        assertEquals(mockArticles, (state as ArticleListState.Success).articles)
    }

    @Test
    fun `loadArticles error sets state to Error`() = runTest {
        fakeRepo.shouldThrowError = true

        viewModel = DashboardViewModel(fakeRepo)
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.listState.value
        assertTrue(state is ArticleListState.Error)
        assertEquals("Network error", (state as ArticleListState.Error).message)
    }

    @Test
    fun `triggerGenerateAndRefresh calls trigger and reloads articles`() = runTest {
        val mockArticles = listOf(Article(id = "1", title = "A1", body = "B1", summary = "S1"))
        fakeRepo.articlesToReturn = mockArticles
        viewModel = DashboardViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Reset fake state for the refresh
        fakeRepo.articlesToReturn = mockArticles + Article(id = "2", title = "A2", body = "B2", summary = "S2")
        
        viewModel.triggerGenerateAndRefresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.triggerGenerateCalled)
        val state = viewModel.listState.value
        assertTrue(state is ArticleListState.Success)
        assertEquals(2, (state as ArticleListState.Success).articles.size)
    }

    @Test
    fun `loadArticle success sets detailState to Success`() = runTest {
        val article = Article(id = "1", title = "A1", body = "B1", summary = "S1")
        fakeRepo.articlesToReturn = listOf(article)
        viewModel = DashboardViewModel(fakeRepo)
        
        viewModel.loadArticle("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state is ArticleDetailState.Success)
        assertEquals(article, (state as ArticleDetailState.Success).article)
    }
}
