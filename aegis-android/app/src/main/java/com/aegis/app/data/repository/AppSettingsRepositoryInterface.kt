package com.aegis.app.data.repository

import com.aegis.app.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contract for local app settings (Room-backed).
 * Allows ViewModels to be tested with a fake without Room/DAO overhead.
 */
interface AppSettingsRepositoryInterface {
    fun observe(): Flow<AppSettingsEntity?>
    suspend fun get(): AppSettingsEntity
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun isOnboardingComplete(): Boolean
}
