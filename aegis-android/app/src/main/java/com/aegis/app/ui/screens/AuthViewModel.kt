package com.aegis.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                persistSession()
                _uiState.value = AuthState.Success
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                persistSession()
                _uiState.value = AuthState.Success
            } catch (e: Exception) {
                _uiState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthState.Idle
    }

    /**
     * Persists the Supabase session tokens into EncryptedSharedPreferences
     * so they survive process death without re-authentication.
     * Complies with AI_RULES.md §2 — no plaintext token storage.
     */
    private fun persistSession() {
        val session = supabase.auth.currentSessionOrNull() ?: return
        sessionManager.saveSession(
            accessToken  = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAt    = session.expiresAt.toEpochMilliseconds()
        )
    }
}

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
