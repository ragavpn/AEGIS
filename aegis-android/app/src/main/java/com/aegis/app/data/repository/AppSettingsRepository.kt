package com.aegis.app.data.repository

import com.aegis.app.data.local.dao.AppSettingsDao
import com.aegis.app.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for local app settings stored in Room.
 * Currently manages the onboarding-complete flag.
 * Extend with additional local flags as needed.
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    private val dao: AppSettingsDao
) : AppSettingsRepositoryInterface {
    /** Reactive stream of the current settings. */
    override fun observe(): Flow<AppSettingsEntity?> = dao.observe()

    override suspend fun get(): AppSettingsEntity = dao.get() ?: AppSettingsEntity()

    override suspend fun setOnboardingComplete(complete: Boolean) {
        val current = dao.get() ?: AppSettingsEntity()
        dao.upsert(current.copy(onboardingComplete = complete))
    }

    override suspend fun isOnboardingComplete(): Boolean = get().onboardingComplete
}
