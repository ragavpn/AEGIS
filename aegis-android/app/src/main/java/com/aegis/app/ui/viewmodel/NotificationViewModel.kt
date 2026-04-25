package com.aegis.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.app.data.model.Notification
import com.aegis.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NotificationListState {
    object Loading : NotificationListState()
    data class Success(val notifications: List<Notification>) : NotificationListState()
    data class Error(val message: String) : NotificationListState()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<NotificationListState>(NotificationListState.Loading)
    val listState: StateFlow<NotificationListState> = _listState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _listState.value = NotificationListState.Loading
            try {
                val notifications = repository.getNotifications()
                _listState.value = NotificationListState.Success(notifications)
            } catch (e: Exception) {
                _listState.value = NotificationListState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(notificationId)
                // Optimistically update list
                val currentState = _listState.value
                if (currentState is NotificationListState.Success) {
                    val updated = currentState.notifications.map { 
                        if (it.id == notificationId) it.copy(isRead = true) else it 
                    }
                    _listState.value = NotificationListState.Success(updated)
                }
            } catch (e: Exception) {
                // Ignore failure for mark as read for now
            }
        }
    }
}
