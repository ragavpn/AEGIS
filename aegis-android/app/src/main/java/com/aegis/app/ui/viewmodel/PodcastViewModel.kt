package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.api.AegisApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PodcastState {
    object Idle : PodcastState()
    object Generating : PodcastState()
    data class Ready(val audioUrl: String) : PodcastState()
    data class Error(val message: String) : PodcastState()
}

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val apiService: AegisApiService
) : ViewModel() {

    private val _state = MutableStateFlow<PodcastState>(PodcastState.Idle)
    val state: StateFlow<PodcastState> = _state.asStateFlow()

    fun generatePodcast(articleId: String, durationScale: String = "default") {
        if (_state.value is PodcastState.Generating || _state.value is PodcastState.Ready) return

        viewModelScope.launch {
            _state.value = PodcastState.Generating
            try {
                val response = apiService.generatePodcast(mapOf("article_id" to articleId, "duration_scale" to durationScale))
                val audioUrl = response["audio_url"]
                if (audioUrl != null) {
                    _state.value = PodcastState.Ready(audioUrl)
                } else {
                    _state.value = PodcastState.Error("Failed to get audio URL")
                }
            } catch (e: Exception) {
                _state.value = PodcastState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
