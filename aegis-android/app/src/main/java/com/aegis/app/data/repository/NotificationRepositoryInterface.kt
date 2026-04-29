package com.aegis.app.data.repository

import com.aegis.app.data.model.Notification

/**
 * Contract for notification data access.
 * Allows ViewModels to be tested with a fake without requiring Supabase.
 */
interface NotificationRepositoryInterface {
    suspend fun getNotifications(): List<Notification>
    suspend fun markAsRead(notificationId: String)
}
