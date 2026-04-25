package com.aegis.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aegis.app.data.local.dao.AppSettingsDao
import com.aegis.app.data.local.entity.AppSettingsEntity

/**
 * Aegis local Room database.
 *
 * Version history:
 *  1 — initial schema: app_settings table
 *
 * exportSchema = false for now (enable and add schema export dir before release).
 */
@Database(
    entities = [AppSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
}
