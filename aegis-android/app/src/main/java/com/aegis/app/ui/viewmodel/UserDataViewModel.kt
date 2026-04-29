package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.model.ALL_MODULES
import com.aegis.app.data.model.ArticleInteraction
import com.aegis.app.data.model.UserPreferences
import com.aegis.app.data.repository.AppSettingsRepository
import com.aegis.app.data.repository.UserDataRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Article Interaction ───────────────────────────────────────────────────────
@HiltViewModel
class ArticleInteractionViewModel @Inject constructor(
    private val repo: UserDataRepositoryInterface
) : ViewModel() {

    private val _interaction = MutableStateFlow<ArticleInteraction?>(null)
    val interaction: StateFlow<ArticleInteraction?> = _interaction.asStateFlow()

    fun load(articleId: String) {
        viewModelScope.launch {
            _interaction.value = repo.getInteraction(articleId)
                ?: ArticleInteraction(articleId = articleId)
        }
    }

    fun toggleLike(articleId: String) {
        val current = _interaction.value ?: ArticleInteraction(articleId = articleId)
        val updated = current.copy(liked = !(current.liked ?: false))
        _interaction.value = updated
        viewModelScope.launch { repo.upsertInteraction(updated) }
    }

    fun toggleBookmark(articleId: String) {
        val current = _interaction.value ?: ArticleInteraction(articleId = articleId)
        val updated = current.copy(bookmarked = !current.bookmarked)
        _interaction.value = updated
        viewModelScope.launch { repo.upsertInteraction(updated) }
    }

    fun recordReadDuration(articleId: String, durationSeconds: Int) {
        val current = _interaction.value ?: ArticleInteraction(articleId = articleId)
        val updated = current.copy(readDurationSeconds = current.readDurationSeconds + durationSeconds)
        _interaction.value = updated
        viewModelScope.launch { repo.upsertInteraction(updated) }
    }

    /**
     * Records the percentage (0–100) of the article body scrolled when the user navigates away.
     * Only updates the stored value if the new depth exceeds the previous maximum.
     */
    fun recordScrollDepth(articleId: String, scrollDepthPercent: Int) {
        val current = _interaction.value ?: ArticleInteraction(articleId = articleId)
        val newDepth = maxOf(current.scrollDepthPercent, scrollDepthPercent)
        val updated = current.copy(scrollDepthPercent = newDepth)
        _interaction.value = updated
        viewModelScope.launch { repo.upsertInteraction(updated) }
    }
}

// ── User Preferences ──────────────────────────────────────────────────────────
@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val repo: UserDataRepositoryInterface,
    private val appSettingsRepo: AppSettingsRepository
) : ViewModel() {

    private val _prefs = MutableStateFlow(UserPreferences(modules = ALL_MODULES))
    val prefs: StateFlow<UserPreferences> = _prefs.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _prefs.value = repo.getPreferences() ?: UserPreferences(modules = ALL_MODULES)
            _loading.value = false
        }
    }

    fun toggleModule(module: String) {
        val current = _prefs.value
        val updated = if (current.modules.contains(module)) {
            current.copy(modules = current.modules - module)
        } else {
            current.copy(modules = current.modules + module)
        }
        _prefs.value = updated
    }

    fun setNotificationFlash(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(notificationFlash = enabled)
    }

    fun setNotificationPriority(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(notificationPriority = enabled)
    }

    fun setNotificationRoutine(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(notificationRoutine = enabled)
    }

    fun setTtsSpeed(speed: Double) {
        _prefs.value = _prefs.value.copy(ttsSpeed = speed)
    }

    fun setDndEnabled(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(dndEnabled = enabled)
    }

    fun save() {
        viewModelScope.launch {
            repo.upsertPreferences(_prefs.value)
            // Write the Room flag so GatekeeperScreen can route instantly on next cold start (offline-safe)
            appSettingsRepo.setOnboardingComplete(true)
            _saved.value = true
        }
    }

    fun resetSaved() { _saved.value = false }
}
