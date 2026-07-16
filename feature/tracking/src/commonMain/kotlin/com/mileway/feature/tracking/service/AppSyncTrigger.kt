package com.mileway.feature.tracking.service

import com.mileway.feature.tracking.repository.CurrentTrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * PLAN_V34 P1: app-scoped outbox flush triggers.
 *
 * PLAN_V33 A4 wired connectivity-edge + ON_START drains into [SyncStatusViewModel], but that
 * ViewModel only exists while the Saved Tracks screen does — a user who tracked offline and never
 * revisits that screen would keep unsynced points forever. This class hosts the same two triggers
 * at APPLICATION scope: [start] collects the offline→online edge for the whole process lifetime,
 * and [onAppForeground] is called from the platform's app-foreground hook (Android
 * ActivityLifecycleCallbacks 0→1 started transition; iOS applicationDidBecomeActive).
 *
 * [SyncStatusViewModel]'s own triggers stay — `drain()`'s min-gap + isDraining guards make the
 * overlap a no-op. The injected [scope] MUST be main-dispatcher-bound: [LocationDataSyncer]'s
 * isDraining guard is a plain Boolean that assumes all callers share one dispatcher.
 */
class AppSyncTrigger(
    private val syncer: LocationDataSyncer,
    private val milesSyncer: MilesSubmitSyncer,
    private val currentTrackRepo: CurrentTrackRepository,
    private val isConnectedFlow: Flow<Boolean>,
    private val scope: CoroutineScope,
) {
    /** Call once at app startup (after DI is up). Collects connectivity edges until [scope] dies. */
    fun start() {
        scope.launch {
            isConnectedFlow
                .distinctUntilChanged()
                .drop(1) // skip the "already online at startup" emission
                .filter { connected -> connected }
                .collect { drainAll() }
        }
    }

    /** Call from the platform app-foreground hook. Safe to call repeatedly (min-gap guarded). */
    fun onAppForeground() {
        scope.launch { drainAll() }
    }

    private suspend fun drainAll() {
        val token = currentTrackRepo.getCurrentTrackDataRawAsync().getOrNull()?.token
        if (!token.isNullOrEmpty()) syncer.drain(token)
        milesSyncer.drain()
    }
}
