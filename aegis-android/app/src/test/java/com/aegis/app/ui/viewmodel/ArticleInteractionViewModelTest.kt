package com.aegis.app.ui.viewmodel

import com.aegis.app.data.model.ArticleInteraction
import com.aegis.app.data.model.UserPreferences
import com.aegis.app.data.repository.UserDataRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake repository that implements the interface without any real Supabase calls.
 * This is the idiomatic Kotlin approach — no Mockito needed for simple fakes.
 * Satisfies AI_RULES.md §5: at least 1 unit test per ViewModel,
 * mocking the repository layer so tests are fully isolated.
 */
class FakeUserDataRepository : UserDataRepositoryInterface {
    val upsertedInteractions = mutableListOf<ArticleInteraction>()
    private var storedInteraction: ArticleInteraction? = null

    override suspend fun getInteraction(articleId: String): ArticleInteraction? =
        storedInteraction

    override suspend fun upsertInteraction(interaction: ArticleInteraction) {
        storedInteraction = interaction
        upsertedInteractions.add(interaction)
    }

    override suspend fun getPreferences(): UserPreferences? = null
    override suspend fun upsertPreferences(prefs: UserPreferences) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleInteractionViewModelTest {

    private lateinit var viewModel: ArticleInteractionViewModel
    private lateinit var fakeRepo: FakeUserDataRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeUserDataRepository()
        viewModel = ArticleInteractionViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLike sets liked to true and persists via repository`() = runTest {
        val articleId = "test-article-1"

        viewModel.toggleLike(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.interaction.value
        assertNotNull(state)
        assertEquals(true, state?.liked)
        assertEquals(articleId, state?.articleId)
        assertEquals(1, fakeRepo.upsertedInteractions.size)
    }

    @Test
    fun `toggleLike called twice flips liked back to false`() = runTest {
        val articleId = "test-article-2"

        viewModel.toggleLike(articleId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleLike(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.interaction.value?.liked)
        assertEquals(2, fakeRepo.upsertedInteractions.size)
    }

    @Test
    fun `recordReadDuration accumulates across multiple calls`() = runTest {
        val articleId = "test-article-3"

        viewModel.recordReadDuration(articleId, 10)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.recordReadDuration(articleId, 15)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(25, viewModel.interaction.value?.readDurationSeconds)
        assertEquals(2, fakeRepo.upsertedInteractions.size)
    }

    @Test
    fun `recordScrollDepth only updates when new depth exceeds previous maximum`() = runTest {
        val articleId = "test-article-4"

        viewModel.recordScrollDepth(articleId, 50)
        assertEquals(50, viewModel.interaction.value?.scrollDepthPercent)

        // Lower value should NOT reduce the recorded max
        viewModel.recordScrollDepth(articleId, 30)
        assertEquals(50, viewModel.interaction.value?.scrollDepthPercent)

        // Higher value should update
        viewModel.recordScrollDepth(articleId, 80)
        assertEquals(80, viewModel.interaction.value?.scrollDepthPercent)

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `toggleBookmark sets bookmarked to true and persists via repository`() = runTest {
        val articleId = "test-article-5"

        viewModel.toggleBookmark(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.interaction.value?.bookmarked == true)
        assertEquals(1, fakeRepo.upsertedInteractions.size)
    }
}
