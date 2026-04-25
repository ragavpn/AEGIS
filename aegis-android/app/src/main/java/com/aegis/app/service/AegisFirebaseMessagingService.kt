package com.aegis.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aegis.app.MainActivity
import com.aegis.app.R
import com.aegis.app.data.repository.ChatRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AegisFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var supabaseClient: SupabaseClient

    companion object {
        const val CHANNEL_ID_FLASH    = "aegis_flash"
        const val CHANNEL_ID_PRIORITY = "aegis_priority"
        const val CHANNEL_ID_ROUTINE  = "aegis_routine"
        const val EXTRA_DEEP_LINK     = "deep_link"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AegisFCM", "New FCM token: $token")
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                chatRepository.registerDeviceToken(userId, token)
                Log.d("AegisFCM", "Token registered for user $userId")
            } catch (e: Exception) {
                Log.e("AegisFCM", "Failed to register token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title   = message.notification?.title ?: message.data["title"] ?: "AEGIS Alert"
        val body    = message.notification?.body  ?: message.data["body"]  ?: ""
        val deepLink = message.data["deep_link"]
        val type    = message.data["type"] ?: "routine"

        Log.d("AegisFCM", "Message received: $title | deep_link=$deepLink")

        showNotification(title, body, deepLink, type)
    }

    private fun showNotification(title: String, body: String, deepLink: String?, type: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(nm)

        // Build deep-link intent — MainActivity reads EXTRA_DEEP_LINK and navigates
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!deepLink.isNullOrBlank()) putExtra(EXTRA_DEEP_LINK, deepLink)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (type) {
            "flash"    -> CHANNEL_ID_FLASH
            "priority" -> CHANNEL_ID_PRIORITY
            else       -> CHANNEL_ID_ROUTINE
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (type == "flash") NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        listOf(
            Triple(CHANNEL_ID_FLASH,    "FLASH Alerts",    NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_ID_PRIORITY, "PRIORITY Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_ID_ROUTINE,  "Routine Updates", NotificationManager.IMPORTANCE_LOW)
        ).forEach { (id, name, importance) ->
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(NotificationChannel(id, name, importance))
            }
        }
    }
}
