package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.model.display.TrackingSystemFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Live, in-memory telemetry the foreground tracking service publishes for the UI (C.2b). Unlike the
 * persisted `CurrentTrackData` session, this carries transient signals the DB doesn't keep, the
 * adaptive GPS interval, charging state, and the latest diagnostic [lastEvent], so the gauge can react
 * to them in real time without a Room round-trip.
 */
data class TrackingSnapshot(
    val state: TrackingState = TrackingState.READY,
    val token: String? = null,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val speedMps: Double = 0.0,
    val avgSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val totalPoints: Int = 0,
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    val currentIntervalMs: Long = 0L,
    val lastEvent: EventType? = null,
    /** C.2b: 0..100 live fix-quality score (scored per fix by TrackingQualityScorer); 100 = ideal. */
    val qualityScore: Int = 100,
    /** Cumulative distance (m) rejected as GPS spikes this session. */
    val spikeDistanceM: Double = 0.0,
    /** False once GPS is disabled / no fixes are arriving. */
    val isGpsAvailable: Boolean = true,
    /** C.2g: true during the brief grace window after a resume (spike/auto-discard suppressed). */
    val inResumeGrace: Boolean = false,
    /** Active system-health issues (power-saver, mock, etc.). */
    val systemFlags: TrackingSystemFlags = TrackingSystemFlags(),
)

/**
 * Read-only live view of the foreground tracking session (C.2b/C.3). The ViewModel observes
 * [trackingState]; the service writes it through [TrackingStatePublisher].
 */
interface TrackingServiceApi {
    val trackingState: StateFlow<TrackingSnapshot>
}

/**
 * In-process publisher of [TrackingSnapshot]. Held as a Koin singleton so the foreground service writes
 * and the ViewModel reads the same flow.
 *
 * Chosen over a bound-service `Binder`: the consumer ([com.mileway.feature.tracking.viewmodel.TrackMilesViewModel])
 * lives in the same process, so a shared [StateFlow] is the lifecycle-safe, idiomatic channel, no
 * `ServiceConnection` to leak across config changes, and it is unit-testable as a pure flow. (A Binder
 * only earns its keep for cross-process IPC, which this offline demo never does.)
 */
class TrackingStatePublisher : TrackingServiceApi {
    private val _trackingState = MutableStateFlow(TrackingSnapshot())
    override val trackingState: StateFlow<TrackingSnapshot> = _trackingState.asStateFlow()

    fun update(transform: (TrackingSnapshot) -> TrackingSnapshot) = _trackingState.update(transform)

    fun reset() {
        _trackingState.value = TrackingSnapshot()
    }
}
