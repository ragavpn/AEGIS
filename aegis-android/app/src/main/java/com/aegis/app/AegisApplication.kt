package com.aegis.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AegisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val flashChannel = NotificationChannel(
                "aegis_flash",
                "Flash Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical geopolitical flashes"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            val priorityChannel = NotificationChannel(
                "aegis_priority",
                "Priority Intelligence",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Important updates requiring attention"
                enableLights(true)
                lightColor = Color.YELLOW
            }

            val routineChannel = NotificationChannel(
                "aegis_routine",
                "Routine Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily briefings and standard articles"
                enableLights(false)
            }

            notificationManager.createNotificationChannels(
                listOf(flashChannel, priorityChannel, routineChannel)
            )
        }
    }
}
