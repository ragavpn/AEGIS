package com.aegis.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and retrieves the Supabase JWT using EncryptedSharedPreferences backed
 * by the Android Keystore system. Complies with AI_RULES.md §2 (no plaintext tokens).
 *
 * The Supabase Kotlin SDK manages its own in-memory session, but on cold start we
 * restore the token here and call supabase.auth.importSession() in AegisApplication
 * so the SDK always reflects the persisted session.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "aegis_secure_prefs"
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT    = "expires_at"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Save full session tokens after a successful sign-in or token refresh. */
    fun saveSession(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN,  accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT,      expiresAt)
            .apply()
    }

    /** Returns true when a stored access token exists (user was previously signed in). */
    fun hasSession(): Boolean = prefs.contains(KEY_ACCESS_TOKEN)

    fun getAccessToken():  String? = prefs.getString(KEY_ACCESS_TOKEN,  null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getExpiresAt():    Long    = prefs.getLong(KEY_EXPIRES_AT, 0L)

    /** Wipe stored tokens on sign-out. */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
