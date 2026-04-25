package com.aegis.app.di

import android.content.Context
import androidx.room.Room
import com.aegis.app.data.local.AegisDatabase
import com.aegis.app.data.local.dao.AppSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AegisDatabase =
        Room.databaseBuilder(
            context,
            AegisDatabase::class.java,
            "aegis_local.db"
        )
            .fallbackToDestructiveMigration() // OK for Phase 6-7; add real migrations before 1.0
            .build()

    @Provides
    @Singleton
    fun provideAppSettingsDao(db: AegisDatabase): AppSettingsDao = db.appSettingsDao()
}
