package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.profile.data.NotificationRecord
import com.mileway.feature.profile.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V22 P6.5: state for
 * [NotificationCentreScreen][com.mileway.feature.profile.ui.screens.NotificationCentreScreen] —
 * real Room-backed persistence replacing the screen's previous `remember { mutableStateOf(
 * NOTIFICATIONS) }` seed. [unreadCount] is always derived from [notifications], never a hardcoded
 * string like the screen's old "174 unread" topbar subtitle.
 */
data class NotificationUiState(
    val notifications: List<NotificationRecord> = emptyList(),
) {
    val unreadCount: Int get() = notifications.count { it.isUnread }
}

/** One-shot navigation effect fired when a notification card is tapped. */
sealed interface NotificationEffect {
    data class OpenDeepLink(val deeplink: String) : NotificationEffect
}

class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<NotificationEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<NotificationEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        repository.observeAll().onEach { list -> _state.update { it.copy(notifications = list) } }.launchIn(viewModelScope)
    }

    /** Toggles a single entry's read/unread state. */
    fun setUnread(
        id: String,
        isUnread: Boolean,
    ) {
        viewModelScope.launch { repository.setUnread(id, isUnread) }
    }

    /** Marks every notification read (the topbar's "Mark all read" action). */
    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    /** Card tap: marks the entry read and emits its deep-link for the host nav graph to resolve. */
    fun open(notif: NotificationRecord) {
        viewModelScope.launch {
            if (notif.isUnread) repository.setUnread(notif.id, false)
            _effect.emit(NotificationEffect.OpenDeepLink(notif.deeplink))
        }
    }
}
