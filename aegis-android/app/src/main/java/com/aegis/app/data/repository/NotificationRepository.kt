package com.aegis.app.data.repository

import com.aegis.app.data.model.Notification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getNotifications(): List<Notification> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        return supabase
            .from("notifications")
            .select {
                filter {
                    eq("user_id", user.id)
                }
                order("created_at", order = Order.DESCENDING)
                limit(50)
            }
            .decodeList<Notification>()
    }

    suspend fun markAsRead(notificationId: String) {
        supabase.from("notifications").update(
            {
                set("is_read", true)
            }
        ) {
            filter {
                eq("id", notificationId)
            }
        }
    }
}
