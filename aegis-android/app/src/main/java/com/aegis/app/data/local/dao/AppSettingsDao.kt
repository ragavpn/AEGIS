package com.aegis.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegis.app.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app-level settings stored locally via Room.
 * Uses a singleton row (id = 1) with REPLACE conflict strategy for upsert behaviour.
 */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}
