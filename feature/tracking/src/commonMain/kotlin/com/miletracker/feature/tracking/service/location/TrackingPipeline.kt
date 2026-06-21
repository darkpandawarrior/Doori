@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.miletracker.feature.tracking.service.location

import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.util.KalmanSmoother
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Platform-independent GPS fix. Sources convert their native location into this so the
 * [LocationProcessor] stays pure Kotlin (unit-testable without Android/Play Services).
 */
data class GpsFix(
    val lat: Double,
    val lng: Double,
    val timeMs: Long,
    val speedMps: Float = 0f,
    val accuracyM: Float = 0f,
    val bearingDeg: Float = 0f,
    val altitudeM: Double = 0.0,
    val provider: String = "fused",
    val isMock: Boolean = false,
)

/** Latest IMU readings attached to each persisted point (zeros if no sensors). */
data class SensorSnapshot(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
)

/** Result of processing one fix: the row to persist plus classification flags. */
data class ProcessResult(
    val location: LocationData,
    val displacementM: Double,
    val isAbnormal: Boolean,
    val isMock: Boolean,
    val countedTowardDistance: Boolean,
)

/**
 * Running accumulators for a session, exposed so the service can persist live stats.
 */
data class TrackStats(
    val totalPoints: Int,
    val originalDistanceM: Double,
    val cleanedDistanceM: Double,
    val abnormalDistanceM: Double,
    val mockDistanceM: Double,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
)

/**
 * Advanced point-by-point processor: distance accumulation (Haversine), GPS-jitter
 * suppression, mock-location handling, and spike/abnormal detection (implausible implied
 * speed). Distance is split into original / cleaned / abnormal / mock buckets so the UI can
 * present a trustworthy "cleaned" distance while retaining the raw figure.
 *
 * Pure Kotlin, no Android dependency, so it is fully covered by JVM unit tests.
 */
class LocationProcessor(
    // ~252 km/h, anything faster (for normal sampling) is a GPS spike
    private val maxPlausibleSpeedMps: Double = 70.0,
    private val deviceModel: String = "",
    private val appVersionName: String = "",
    // C.1g: when true, each fix's lat/lng is run through the shared KalmanSmoother before
    // distance/classification. Off by default, no change to the existing pipeline behaviour.
    private val enableKalman: Boolean = false,
    initialStats: TrackStats? = null,
) {
    private var last: GpsFix? = null
    private var firstFix: GpsFix? = null

    // C.1g: shared smoother, reset at journey start (a fresh processor = a fresh smoother).
    private val kalman = KalmanSmoother().also { if (enableKalman) it.reset() }

    var totalPoints = 0
        private set
    var originalDistanceM = 0.0
        private set
    var cleanedDistanceM = 0.0
        private set
    var abnormalDistanceM = 0.0
        private set
    var mockDistanceM = 0.0
        private set

    /** Distance from hard-gate teleport spikes (>5 km in a single normal-sampling step). */
    var spikeDistanceM = 0.0
        private set

    /** Consecutive non-abnormal fixes; resets to 0 on each abnormal fix (C.1b). */
    var consecutiveNormalCount = 0
        private set

    var maxSpeedMps = 0.0
        private set
    private var speedSum = 0.0
    private var speedCount = 0

    /** Rolling window of the last [SPEED_HISTORY_SIZE] processed-fix speeds (m/s), C.1c. */
    private val recentSpeedHistory = ArrayDeque<Double>()

    init {
        // Resume accumulators from a persisted session (e.g. after the tracking service is
        // restarted across a reboot). `last` stays null so the first fix of the resumed
        // segment is an anchor: the gap travelled while not tracking never counts as distance.
        initialStats?.let { s ->
            totalPoints = s.totalPoints
            originalDistanceM = s.originalDistanceM
            cleanedDistanceM = s.cleanedDistanceM
            abnormalDistanceM = s.abnormalDistanceM
            mockDistanceM = s.mockDistanceM
            maxSpeedMps = s.maxSpeedMps
            // The exact speed-sample count isn't persisted; weighting by totalPoints keeps
            // the running average continuous across the restart.
            speedSum = s.avgSpeedMps * s.totalPoints
            speedCount = s.totalPoints
        }
    }

    val avgSpeedMps: Double get() = if (speedCount > 0) speedSum / speedCount else 0.0
    val firstLat: Double? get() = firstFix?.lat
    val firstLng: Double? get() = firstFix?.lng

    fun stats() =
        TrackStats(
            totalPoints = totalPoints,
            originalDistanceM = originalDistanceM,
            cleanedDistanceM = cleanedDistanceM,
            abnormalDistanceM = abnormalDistanceM,
            mockDistanceM = mockDistanceM,
            avgSpeedMps = avgSpeedMps,
            maxSpeedMps = maxSpeedMps,
        )

    /**
     * Feed a fix. Returns the row to persist, or null when the fix is suppressed as jitter.
     * @param isPaused when true the point is recorded but does not add to the cleaned distance.
     */
    fun process(
        fix: GpsFix,
        isPaused: Boolean,
        sensors: SensorSnapshot = SensorSnapshot(),
    ): ProcessResult? {
        val prev = last
        // C.1g: smooth lat/lng up front so distance + classification use the filtered position.
        // With Kalman off, effFix === fix and the pipeline is byte-for-byte unchanged.
        val effFix =
            if (enableKalman) {
                val (sLat, sLng) = kalman.smooth(fix.lat, fix.lng, fix.accuracyM, fix.timeMs)
                fix.copy(lat = sLat, lng = sLng)
            } else {
                fix
            }
        if (firstFix == null) firstFix = effFix

        val displacement = if (prev != null) haversineMeters(prev.lat, prev.lng, effFix.lat, effFix.lng) else 0.0
        val dtSec = if (prev != null) max(1L, (fix.timeMs - prev.timeMs) / 1000L) else 1L
        val impliedSpeed = displacement / dtSec

        // C.1a/C.1c: speed-adaptive jitter suppression (only for normal sampling, never across a
        // time gap). A small wander below the speed-tuned gate is dropped while parked, but a fix
        // is let through when recent history shows real movement (stationary-with-momentum). Mock
        // fixes are never suppressed (handled in their own bucket). The anchor is kept so a later
        // genuine move is measured from the last *persisted* point.
        if (prev != null && !fix.isMock && dtSec < GAP_MIN_SEC) {
            val gate = minDisplacementForSpeed(fix.speedMps.toDouble())
            val stationaryMicroJitter =
                fix.speedMps < STATIONARY_SPEED_MPS && displacement < STATIONARY_JITTER_M
            if ((displacement < gate || stationaryMicroJitter) && !hasMovementHistory()) {
                return null
            }
        }

        // C.1b/C.1d: abnormal classification, 5 km hard teleport gate for normal sampling, plus
        // gap-recovery speed tiers that relax the cap as the time gap grows (and a 10 km distance
        // gate beyond 6 h). Gap fixes that pass the tier are counted toward distance, not flagged.
        val abnormal = prev != null && isAbnormal(displacement, impliedSpeed, dtSec)
        val isHardSpike = prev != null && dtSec < GAP_MIN_SEC && displacement > SPIKE_HARD_GATE_M
        var counted = false

        if (prev != null) {
            originalDistanceM += displacement
            when {
                fix.isMock -> mockDistanceM += displacement
                abnormal -> {
                    abnormalDistanceM += displacement
                    if (isHardSpike) spikeDistanceM += displacement
                }
                isPaused -> { /* recorded but excluded from cleaned distance */ }
                else -> {
                    cleanedDistanceM += displacement
                    counted = true
                }
            }
            // C.1b: track a clean streak so the service can reset the abnormal anchor after 3.
            consecutiveNormalCount = if (abnormal) 0 else consecutiveNormalCount + 1
        }

        if (!isPaused && !abnormal && !fix.isMock) {
            val sp = fix.speedMps.toDouble()
            speedSum += sp
            speedCount++
            if (sp > maxSpeedMps) maxSpeedMps = sp
        }

        // C.1c: record this processed fix's speed in the rolling movement-history window.
        recentSpeedHistory.addLast(fix.speedMps.toDouble())
        if (recentSpeedHistory.size > SPEED_HISTORY_SIZE) recentSpeedHistory.removeFirst()

        last = effFix
        totalPoints++

        val row =
            LocationData(
                activity = if (isPaused) "PAUSED" else "DRIVING",
                speed = fix.speedMps,
                lat = effFix.lat,
                lng = effFix.lng,
                // set by the service before insert
                token = "",
                date = fix.timeMs,
                displacement = displacement,
                accuracy = fix.accuracyM,
                isMock = fix.isMock,
                isAbnormal = abnormal,
                isPaused = isPaused,
                // set by the service from BatteryManager
                batteryPercentage = 0.0,
                gyroscopeX = sensors.gyroX,
                gyroscopeY = sensors.gyroY,
                gyroscopeZ = sensors.gyroZ,
                accelerometerX = sensors.accelX,
                accelerometerY = sensors.accelY,
                accelerometerZ = sensors.accelZ,
                provider = fix.provider,
                bearing = fix.bearingDeg,
                altitude = fix.altitudeM,
                locationTime = fix.timeMs,
                deviceModel = deviceModel,
                appVersionName = appVersionName,
            )
        return ProcessResult(
            location = row,
            displacementM = displacement,
            isAbnormal = abnormal,
            isMock = fix.isMock,
            countedTowardDistance = counted,
        )
    }

    /**
     * C.1a: minimum displacement (m) a fix must cover to escape jitter suppression, tuned to the
     * current speed band. Faster travel expects (and tolerates) larger steps between fixes.
     */
    private fun minDisplacementForSpeed(speedMps: Double): Double =
        when {
            speedMps < WALKING_MAX_MPS -> WALKING_JITTER_M
            speedMps < CYCLING_MAX_MPS -> CYCLING_JITTER_M
            else -> DRIVING_JITTER_M
        }

    /** C.1c: true when the recent window shows sustained movement, so a small step isn't jitter. */
    private fun hasMovementHistory(): Boolean = recentSpeedHistory.isNotEmpty() && recentSpeedHistory.average() >= MOVEMENT_HISTORY_MPS

    /**
     * C.1b/C.1d: classify a step as abnormal. For normal sampling (<30 s) a 5 km jump is an instant
     * teleport and any implied speed above the plausible cap is a spike. For recognised gaps the cap
     * is relaxed by tier (the longer the gap, the larger a plausible jump), and beyond 6 h a flat
     * 10 km distance gate replaces the speed test.
     */
    private fun isAbnormal(
        displacement: Double,
        impliedSpeed: Double,
        dtSec: Long,
    ): Boolean =
        when {
            dtSec < GAP_MIN_SEC ->
                displacement > SPIKE_HARD_GATE_M || impliedSpeed > maxPlausibleSpeedMps
            dtSec <= GAP_5M_SEC -> impliedSpeed > GAP_TIER_5M_MPS
            dtSec <= GAP_1H_SEC -> impliedSpeed > GAP_TIER_1H_MPS
            dtSec <= GAP_6H_SEC -> impliedSpeed > GAP_TIER_6H_MPS
            else -> displacement > GAP_MAX_DISTANCE_M
        }

    companion object {
        // C.1a: speed-band jitter gates.
        private const val WALKING_MAX_MPS = 2.5 // < ~9 km/h
        private const val CYCLING_MAX_MPS = 7.0 // < ~25 km/h
        private const val WALKING_JITTER_M = 2.0
        private const val CYCLING_JITTER_M = 3.0
        private const val DRIVING_JITTER_M = 5.0
        private const val STATIONARY_SPEED_MPS = 1.2
        private const val STATIONARY_JITTER_M = 1.2

        // C.1c: movement-history window.
        private const val SPEED_HISTORY_SIZE = 5
        private const val MOVEMENT_HISTORY_MPS = 1.5

        // C.1b: instant-teleport hard gate.
        private const val SPIKE_HARD_GATE_M = 5_000.0

        // C.1d: gap-recovery tiers (seconds + relaxed speed caps, m/s).
        private const val GAP_MIN_SEC = 30L
        private const val GAP_5M_SEC = 300L
        private const val GAP_1H_SEC = 3_600L
        private const val GAP_6H_SEC = 21_600L
        private const val GAP_TIER_5M_MPS = 150.0
        private const val GAP_TIER_1H_MPS = 100.0
        private const val GAP_TIER_6H_MPS = 60.0
        private const val GAP_MAX_DISTANCE_M = 10_000.0
    }
}

/** Great-circle distance in metres between two lat/lng points. */
fun haversineMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLng / 2) * sin(dLng / 2)
    return earthRadiusM * (2 * atan2(sqrt(a), sqrt(1 - a)))
}
