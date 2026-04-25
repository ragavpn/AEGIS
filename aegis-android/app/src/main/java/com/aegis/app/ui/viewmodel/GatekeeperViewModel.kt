package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.repository.AppSettingsRepository
import com.aegis.app.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GatekeeperDestination {
    object Pending : GatekeeperDestination()
    object Main : GatekeeperDestination()
    object Onboarding : GatekeeperDestination()
}

/**
 * Handles the two-stage routing logic for GatekeeperScreen.
 *
 * Stage 1 — Local Room (instant, offline-safe):
 *   If onboardingComplete = true, emit Main immediately.
 *
 * Stage 2 — Network fallback (only when Room flag is false):
 *   A returning user who cleared app storage will have the flag = false in Room but
 *   will have a preferences row in Supabase. If found, emit Main and repair the flag.
 *   If not found, emit Onboarding.
 */
@HiltViewModel
class GatekeeperViewModel @Inject constructor(
    private val appSettingsRepo: AppSettingsRepository,
    private val userDataRepo: UserDataRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<GatekeeperDestination>(GatekeeperDestination.Pending)
    val destination: StateFlow<GatekeeperDestination> = _destination.asStateFlow()

    init {
        resolve()
    }

    private fun resolve() {
        viewModelScope.launch {
            // Stage 1: instant Room check — no network required
            if (appSettingsRepo.isOnboardingComplete()) {
                _destination.value = GatekeeperDestination.Main
                return@launch
            }

            // Stage 2: network fallback — does the user already have a preferences row?
            val prefs = runCatching { userDataRepo.getPreferences() }.getOrNull()
            if (prefs?.id != null) {
                // Returning user (e.g. reinstalled app). Repair Room flag for future cold starts.
                appSettingsRepo.setOnboardingComplete(true)
                _destination.value = GatekeeperDestination.Main
            } else {
                _destination.value = GatekeeperDestination.Onboarding
            }
        }
    }
}
