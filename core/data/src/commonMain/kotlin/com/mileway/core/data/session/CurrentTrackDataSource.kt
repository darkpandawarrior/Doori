package com.mileway.core.data.session

import com.mileway.core.data.model.db.CurrentTrackData
import kotlinx.coroutines.flow.Flow

/**
 * PLAN_V24 P10.2 — a current-journey-only override of the mileage-sync settings. When set (and a
 * track is active), it wins over the persisted defaults (the sync-settings plugins in the registry)
 * for the running journey only; it is cleared with the session. This is the reference app's `applyToFutureJourneys`
 * model: a change either updates the persisted default (registry) or just this journey (here).
 */
data class SyncSessionOverride(
    val locationEnabled: Boolean,
    val eventsEnabled: Boolean,
    val debugEventsEnabled: Boolean,
    val v2ApiEnabled: Boolean,
    val intervalMinutes: Int,
) {
    fun encode(): String = "$locationEnabled,$eventsEnabled,$debugEventsEnabled,$v2ApiEnabled,$intervalMinutes"

    companion object {
        fun decode(raw: String?): SyncSessionOverride? {
            val parts = raw?.split(",") ?: return null
            if (parts.size != 5) return null
            val interval = parts[4].toIntOrNull() ?: return null
            return SyncSessionOverride(
                locationEnabled = parts[0].toBooleanStrictOrNull() ?: return null,
                eventsEnabled = parts[1].toBooleanStrictOrNull() ?: return null,
                debugEventsEnabled = parts[2].toBooleanStrictOrNull() ?: return null,
                v2ApiEnabled = parts[3].toBooleanStrictOrNull() ?: return null,
                intervalMinutes = interval,
            )
        }
    }
}

interface CurrentTrackDataSource {
    val currentTrackFlow: Flow<CurrentTrackData>

    /** P10.2: the current-journey sync-settings override (null = follow persisted defaults). */
    val syncSessionOverrideFlow: Flow<SyncSessionOverride?>

    /** P10.2: set/clear the current-journey sync override (null clears it). */
    suspend fun setSyncSessionOverride(override: SyncSessionOverride?)

    suspend fun saveSession(data: CurrentTrackData)

    suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    )

    suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    )

    suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    )

    suspend fun markResumed(token: String)

    suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    )

    suspend fun clearSession()

    suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    )
}
