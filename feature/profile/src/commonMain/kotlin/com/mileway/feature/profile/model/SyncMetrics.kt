package com.mileway.feature.profile.model

/**
 * PLAN_V22 P6.7: local, mock-data shape for Settings' "Sync Diagnostics" card — the same kind of
 * field shape a real sync-status surface would expose, but sourced from an in-memory counter (see
 * `SyncDiagnosticsRepository`) rather than any network call, since Mileway has no backend to sync
 * against yet.
 */
data class SyncMetrics(
    val locationsSynced: Int = 0,
    val eventsSynced: Int = 0,
    val failedAttempts: Int = 0,
    val lastSyncTimeMs: Long? = null,
    val isSyncing: Boolean = false,
    // P10.2: real local staging buckets — rows waiting to be moved to the synced counters. force-sync
    // drains the enabled buckets (see SyncDiagnosticsRepository), so these are real counts, not mock.
    val pendingLocations: Int = 0,
    val pendingEvents: Int = 0,
    val pendingDebugEvents: Int = 0,
    // P10.2: when the next auto-sync would be due (lastSync + interval); null until first sync.
    val nextSyncDueMs: Long? = null,
)

/**
 * PLAN_V24 P10.2: the resolved mileage-sync config a force-sync runs under — which staging buckets
 * to drain and the auto-sync cadence. Resolved by the ViewModel from the sync-settings plugins (the
 * persisted default) layered under any current-journey [override][com.mileway.core.data.session.SyncSessionOverride].
 */
data class SyncConfig(
    val locationEnabled: Boolean = true,
    val eventsEnabled: Boolean = true,
    val debugEventsEnabled: Boolean = false,
    val intervalMinutes: Int = 15,
)
