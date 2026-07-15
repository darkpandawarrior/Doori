package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.network.NetworkMonitor
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.RelativeTimeFormatter
import com.mileway.feature.tracking.service.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
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
        viewModelScope.launch { drainCurrentTrack() }

        // PLAN_V33 A4 (RN lesson): flush the outbox the moment connectivity comes back instead of
        // waiting for the next periodic tick. distinctUntilChanged + drop(1) skips the initial
        // "already online at startup" emission, so only a real offline->online transition fires.
        viewModelScope.launch {
            NetworkMonitor.isConnectedFlow
                .distinctUntilChanged()
                .drop(1)
                .filter { connected -> connected }
                .collect { drainCurrentTrack() }
        }
    }

    /**
     * PLAN_V33 A4 (RN lesson): call from the host screen's `ON_START` observer so a foregrounded
     * app flushes immediately rather than only on the next periodic tick. `drain()`'s own min-gap
     * guard makes this safe to call as often as the screen wants.
     */
    fun onForeground() {
        viewModelScope.launch { drainCurrentTrack() }
    }

    private suspend fun drainCurrentTrack() {
        val token = currentTrackRepo.getCurrentTrackDataRawAsync().getOrNull()?.token
        if (!token.isNullOrEmpty()) syncer.drain(token)
    }
}
