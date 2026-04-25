package com.aegis.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row Room entity that persists app-level flags across sessions.
 * Row ID is always 1 (singleton pattern via upsert with fixed primary key).
 *
 * Fields:
 *  - onboardingComplete: true once the user has submitted module selections
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingComplete: Boolean = false
)
