package com.mileway.core.data.watch

import com.mileway.core.data.model.display.SurfaceSnapshot
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * The phone <-> watch sync contract, on BOTH transports: DataLayer bytes (Android/Wear OS,
 * `applicationContext`/message payload JSON-encoded) and WCSession `applicationContext` JSON
 * (iOS/watchOS). Deliberately a tiny, flat subset of [SurfaceSnapshot] — only the fields a watch
 * face/complication/app actually renders — so both platforms share one wire shape and neither
 * side needs to know about Room entities or the wider domain model.
 *
 * kotlinx-serialization with explicit, stable field names: this is written to a byte/JSON channel
 * both native runtimes decode, so renaming a field here is a breaking wire change.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class WatchSyncPayload(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val todayKm: Double = 0.0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val weekKm: Double = 0.0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val tripCount: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val isTracking: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val isPaused: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val weekGoalProgress: Float = 0f,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val lastTripLabel: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val updatedAtMs: Long = 0L,
    /**
     * Token of the live session, or null when idle. The watch needs it to stop a trip the *phone*
     * started: `TrackingController.stop` ignores a token that does not match the running session,
     * so without this the watch's stop button would be decorative.
     */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val activeToken: String? = null,
)

/** Projects the full [SurfaceSnapshot] down to the watch-relevant subset for wire transport. */
fun SurfaceSnapshot.toWatchPayload(): WatchSyncPayload =
    WatchSyncPayload(
        todayKm = todayDistanceKm,
        weekKm = weekDistanceKm,
        tripCount = weekTrips,
        isTracking = isTracking,
        isPaused = isPaused,
        weekGoalProgress = weekGoalProgress,
        lastTripLabel = lastTripLabel,
        updatedAtMs = lastUpdatedEpochMs,
        activeToken = activeToken,
    )

/**
 * Reconstructs a [SurfaceSnapshot] from a received [WatchSyncPayload] for the watch's own
 * rendering pipeline to consume via the same model the phone widgets use. Fields not carried over
 * the wire (e.g. [SurfaceSnapshot.qualityScore], [SurfaceSnapshot.weekGoalKm],
 * [SurfaceSnapshot.actionRequiredCount], [SurfaceSnapshot.todayTrips]) fall back to their
 * defaults — the watch never needs them directly, only the derived [WatchSyncPayload.weekGoalProgress].
 */
fun WatchSyncPayload.toSurfaceSnapshot(): SurfaceSnapshot =
    SurfaceSnapshot(
        todayDistanceKm = todayKm,
        weekDistanceKm = weekKm,
        weekTrips = tripCount,
        isTracking = isTracking,
        lastUpdatedEpochMs = updatedAtMs,
        isPaused = isPaused,
        weekGoalKm = if (weekGoalProgress > 0f) weekKm / weekGoalProgress else SurfaceSnapshot.DEFAULT_WEEK_GOAL_KM,
        lastTripLabel = lastTripLabel,
        activeToken = activeToken,
    )
