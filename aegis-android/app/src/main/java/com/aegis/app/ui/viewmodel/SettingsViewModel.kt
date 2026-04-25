package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val userEmail: String
        get() = supabase.auth.currentUserOrNull()?.email ?: "Unknown"

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                sessionManager.clearSession() // wipe EncryptedSharedPreferences
                _signedOut.value = true
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
