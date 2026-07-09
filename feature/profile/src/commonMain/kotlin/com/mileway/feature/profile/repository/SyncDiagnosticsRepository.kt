package com.mileway.feature.profile.repository

import com.mileway.feature.profile.model.SyncConfig
import com.mileway.feature.profile.model.SyncMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * PLAN_V22 P6.7 / PLAN_V24 P10.2: local staging store backing Settings' `SyncDiagnosticsCard` — no
 * network call (Mileway has no backend to sync against yet, see CLAUDE.md "The backend"). It holds
 * three real staging buckets (`pendingLocations`/`pendingEvents`/`pendingDebugEvents`); [forceSync]
 * runs a local upload-simulation job that *moves rows* from the enabled buckets into the synced
 * counters (real arithmetic, deterministic — not random). Which buckets drain is gated by the passed
 * [SyncConfig], and the config's interval sets `nextSyncDueMs`. A fixed new batch of "captured since
 * last sync" rows is re-staged each run so the button stays meaningful across taps.
 */
class SyncDiagnosticsRepository(private val clock: Clock = Clock.System) {
    private val _metrics =
        MutableStateFlow(
            SyncMetrics(
                locationsSynced = 128,
                eventsSynced = 42,
                failedAttempts = 1,
                lastSyncTimeMs = null,
                pendingLocations = 17,
                pendingEvents = 6,
                pendingDebugEvents = 3,
            ),
        )
    val metrics: StateFlow<SyncMetrics> = _metrics.asStateFlow()

    /**
     * Runs a local upload-simulation job: marks syncing, waits briefly, then moves rows from each
     * enabled staging bucket into the synced counters and re-stages the next batch. A disabled
     * bucket keeps its pending rows (they are not synced). No-op if nothing is enabled.
     */
    suspend fun forceSync(config: SyncConfig = SyncConfig()) {
        if (!config.locationEnabled && !config.eventsEnabled && !config.debugEventsEnabled) return
        _metrics.update { it.copy(isSyncing = true) }
        delay(SYNC_SIMULATION_DELAY_MS)
        val now = clock.now().toEpochMilliseconds()
        _metrics.update { m ->
            val drainedLocations = if (config.locationEnabled) m.pendingLocations else 0
            // Debug events ride the events counter but come from their own bucket, gated separately.
            val drainedEvents = if (config.eventsEnabled) m.pendingEvents else 0
            val drainedDebug = if (config.debugEventsEnabled) m.pendingDebugEvents else 0
            m.copy(
                locationsSynced = m.locationsSynced + drainedLocations,
                eventsSynced = m.eventsSynced + drainedEvents + drainedDebug,
                pendingLocations = m.pendingLocations - drainedLocations + NEW_LOCATIONS_PER_CYCLE,
                pendingEvents = m.pendingEvents - drainedEvents + NEW_EVENTS_PER_CYCLE,
                pendingDebugEvents = m.pendingDebugEvents - drainedDebug + NEW_DEBUG_EVENTS_PER_CYCLE,
                lastSyncTimeMs = now,
                nextSyncDueMs = now + config.intervalMinutes * 60_000L,
                isSyncing = false,
            )
        }
    }

    private companion object {
        const val SYNC_SIMULATION_DELAY_MS = 900L

        // Deterministic "captured since last sync" batch re-staged each run (real, not random).
        const val NEW_LOCATIONS_PER_CYCLE = 5
        const val NEW_EVENTS_PER_CYCLE = 2
        const val NEW_DEBUG_EVENTS_PER_CYCLE = 1
    }
}
