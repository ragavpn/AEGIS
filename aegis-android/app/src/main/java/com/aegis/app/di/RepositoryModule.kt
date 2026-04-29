package com.aegis.app.di

import com.aegis.app.data.local.dao.AppSettingsDao
import com.aegis.app.data.repository.AppSettingsRepository
import com.aegis.app.data.repository.AppSettingsRepositoryInterface
import com.aegis.app.data.repository.ArticleRepository
import com.aegis.app.data.repository.ArticleRepositoryInterface
import com.aegis.app.data.repository.ChatRepository
import com.aegis.app.data.repository.ChatRepositoryInterface
import com.aegis.app.data.repository.NotificationRepository
import com.aegis.app.data.repository.NotificationRepositoryInterface
import com.aegis.app.data.repository.UserDataRepository
import com.aegis.app.data.repository.UserDataRepositoryInterface
import com.aegis.app.data.api.AegisApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAppSettingsRepository(dao: AppSettingsDao): AppSettingsRepositoryInterface =
        AppSettingsRepository(dao)

    @Provides
    @Singleton
    fun provideUserDataRepository(supabase: SupabaseClient): UserDataRepositoryInterface =
        UserDataRepository(supabase)

    @Provides
    @Singleton
    fun provideArticleRepository(supabase: SupabaseClient, apiService: AegisApiService): ArticleRepositoryInterface =
        ArticleRepository(supabase, apiService)

    @Provides
    @Singleton
    fun provideChatRepository(apiService: AegisApiService): ChatRepositoryInterface =
        ChatRepository(apiService)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        supabase: SupabaseClient,
        apiService: AegisApiService
    ): NotificationRepositoryInterface =
        NotificationRepository(supabase, apiService)
}
