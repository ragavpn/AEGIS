package com.aegis.app.ui.viewmodel

import com.aegis.app.data.local.entity.AppSettingsEntity
import com.aegis.app.data.model.ArticleInteraction
import com.aegis.app.data.model.UserPreferences
import com.aegis.app.data.repository.AppSettingsRepositoryInterface
import com.aegis.app.data.repository.UserDataRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeAppSettingsRepository : AppSettingsRepositoryInterface {
    var onboardingComplete = false
    private val flow = MutableStateFlow<AppSettingsEntity?>(null)

    override fun observe(): Flow<AppSettingsEntity?> = flow

    override suspend fun get(): AppSettingsEntity {
        return AppSettingsEntity(onboardingComplete = onboardingComplete)
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        onboardingComplete = complete
        flow.value = AppSettingsEntity(onboardingComplete = complete)
    }

    override suspend fun isOnboardingComplete(): Boolean {
        return onboardingComplete
    }
}

class FakeUserDataRepositoryGatekeeper : UserDataRepositoryInterface {
    var prefsToReturn: UserPreferences? = null
    var shouldFailGetPrefs = false

    override suspend fun getInteraction(articleId: String): ArticleInteraction? = null

    override suspend fun upsertInteraction(interaction: ArticleInteraction) {}

    override suspend fun getPreferences(): UserPreferences? {
        if (shouldFailGetPrefs) throw Exception("Failed")
        return prefsToReturn
    }

    override suspend fun upsertPreferences(prefs: UserPreferences) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class GatekeeperViewModelTest {
    private lateinit var viewModel: GatekeeperViewModel
    private lateinit var fakeAppRepo: FakeAppSettingsRepository
    private lateinit var fakeUserRepo: FakeUserDataRepositoryGatekeeper
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAppRepo = FakeAppSettingsRepository()
        fakeUserRepo = FakeUserDataRepositoryGatekeeper()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resolve returns Main immediately if onboarding complete in Room`() = runTest {
        fakeAppRepo.onboardingComplete = true

        viewModel = GatekeeperViewModel(fakeAppRepo, fakeUserRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GatekeeperDestination.Main, viewModel.destination.value)
    }

    @Test
    fun `resolve returns Main and fixes Room if Onboarding not complete but remote preferences exist`() = runTest {
        fakeAppRepo.onboardingComplete = false
        fakeUserRepo.prefsToReturn = UserPreferences(id = "1", userId = "u1", modules = emptyList())

        viewModel = GatekeeperViewModel(fakeAppRepo, fakeUserRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GatekeeperDestination.Main, viewModel.destination.value)
        assertEquals(true, fakeAppRepo.onboardingComplete)
    }

    @Test
    fun `resolve returns Onboarding if Onboarding not complete and remote preferences do not exist`() = runTest {
        fakeAppRepo.onboardingComplete = false
        fakeUserRepo.shouldFailGetPrefs = true

        viewModel = GatekeeperViewModel(fakeAppRepo, fakeUserRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GatekeeperDestination.Onboarding, viewModel.destination.value)
        assertEquals(false, fakeAppRepo.onboardingComplete)
    }
}
