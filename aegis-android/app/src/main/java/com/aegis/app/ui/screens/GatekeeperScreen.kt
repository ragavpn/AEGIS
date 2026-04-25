package com.aegis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.ui.viewmodel.GatekeeperDestination
import com.aegis.app.ui.viewmodel.GatekeeperViewModel

/**
 * Routing screen that shows a spinner while resolving where to send the user.
 *
 * Delegates all routing logic to [GatekeeperViewModel], which:
 *  1. Reads the local Room flag first (instant, offline-safe).
 *  2. Falls back to Supabase preferences check if the flag is false.
 */
@Composable
fun GatekeeperScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: GatekeeperViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            GatekeeperDestination.Main       -> onNavigateToMain()
            GatekeeperDestination.Onboarding -> onNavigateToOnboarding()
            GatekeeperDestination.Pending    -> { /* still resolving — stay on spinner */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF3B82F6))
    }
}
