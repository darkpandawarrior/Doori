package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.RelativeTimeFormatter
import com.mileway.feature.tracking.service.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Wave-4 §2.3: drives the "Last synced X ago" + backlog chip on the Saved Tracks screen. */
class SyncStatusViewModel(
    private val syncer: LocationDataSyncer,
    private val currentTrackRepo: CurrentTrackRepository,
) : ViewModel() {
    val chipText: StateFlow<String?> =
        syncer.syncStatus
            .map { status ->
                when (status) {
                    is SyncStatus.Idle -> null
                    is SyncStatus.Syncing -> "Syncing…"
                    is SyncStatus.Synced -> {
                        val nowMs = Clock.System.now().toEpochMilliseconds()
                        val relative = RelativeTimeFormatter.format(status.lastSyncedAtMs, nowMs)
                        val backlogSuffix = if (status.backlogCount > 0) " · ${status.backlogCount} pending" else ""
                        "Last synced $relative$backlogSuffix"
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val token = currentTrackRepo.getCurrentTrackDataRawAsync().getOrNull()?.token
            if (!token.isNullOrEmpty()) syncer.drain(token)
        }
    }
}
