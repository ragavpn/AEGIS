package com.aegis.app.di

import com.aegis.app.data.local.dao.AppSettingsDao
import com.aegis.app.data.repository.AppSettingsRepository
import com.aegis.app.data.repository.ChatRepository
import com.aegis.app.data.repository.UserDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Wires concrete repository implementations into the DI graph.
 * All repositories are singletons so their state (flows, caches) is shared.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAppSettingsRepository(dao: AppSettingsDao): AppSettingsRepository =
        AppSettingsRepository(dao)

    @Provides
    @Singleton
    fun provideUserDataRepository(supabase: SupabaseClient): UserDataRepository =
        UserDataRepository(supabase)
}
