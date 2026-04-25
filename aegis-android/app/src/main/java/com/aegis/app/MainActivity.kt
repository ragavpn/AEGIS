package com.aegis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aegis.app.data.repository.ChatRepository
import com.aegis.app.service.AegisFirebaseMessagingService.Companion.EXTRA_DEEP_LINK
import com.aegis.app.ui.navigation.AegisNavGraph
import com.aegis.app.ui.theme.AegisTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabaseClient: SupabaseClient
    @Inject lateinit var chatRepository: ChatRepository

    // Holds the deep-link route sent from a notification tap
    private val deepLinkRoute = mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("AegisFCM", "Notification permission granted")
            fetchAndRegisterFcmToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Parse deep-link from notification tap (cold or warm start)
        deepLinkRoute.value = intent?.getStringExtra(EXTRA_DEEP_LINK)

        requestNotificationPermission()

        setContent {
            AegisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AegisNavGraph(
                        supabaseClient = supabaseClient,
                        initialDeepLink = deepLinkRoute.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App was already running; update state so the NavGraph reacts
        deepLinkRoute.value = intent.getStringExtra(EXTRA_DEEP_LINK)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    fetchAndRegisterFcmToken()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Below Android 13, permission is granted at install time
            fetchAndRegisterFcmToken()
        }
    }

    private fun fetchAndRegisterFcmToken() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AegisFCM", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("AegisFCM", "FCM token: $token")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    chatRepository.registerDeviceToken(userId, token)
                    Log.d("AegisFCM", "Token registered successfully")
                } catch (e: Exception) {
                    Log.e("AegisFCM", "Token registration failed: ${e.message}")
                }
            }
        }
    }
}