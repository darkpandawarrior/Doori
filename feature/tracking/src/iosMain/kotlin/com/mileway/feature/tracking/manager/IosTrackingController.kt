package com.mileway.feature.tracking.manager

import com.mileway.core.data.model.display.TrackingState
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.service.LocationBatcher
import com.mileway.feature.tracking.service.SystemRecoveryDetector
import com.mileway.feature.tracking.service.TrackingSnapshot
import com.mileway.feature.tracking.service.TrackingStatePublisher
import com.mileway.feature.tracking.service.location.DistanceCalculator
import com.mileway.feature.tracking.service.location.GpsFix
import com.mileway.feature.tracking.service.location.LocationProcessor
import com.siddharth.kmp.appshell.GeoPoint
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.location.QualityInputs
import com.siddharth.kmp.location.TrackingQualityScorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * P-B.1/PLAN_V33 C3: iOS implementation of [TrackingController].
 *
 * Drives [LocationTracker] (CoreLocation via `IosLocationTracker`) for location fix delivery and
 * runs each fix through the same commonMain [LocationProcessor] the Android
 * `LocationTrackingService` uses (Kalman smoothing + Haversine distance accumulation + jitter/spike
 * classification) — [LocationProcessor] has no Android dependency, so no androidMain-only shim was
 * needed here. Accepted points are batched to Room via [LocationBatcher]/[LocationRepository] and a
 * real [TrackingSnapshot] (distance, point count, quality) is published through
 * [TrackingStatePublisher] on every fix. [stop] recomputes the authoritative cleaned distance from
 * the persisted points ([DistanceCalculator.computeCleanedDistance]) and finalizes the
 * [com.mileway.core.data.model.db.SavedTrack] row, mirroring the Android service's `stopAndFinalize`.
 *
 * `GeoPoint` (kmp-toolkit `:app-shell`) exposes lat/lng/accuracy/timestamp only — no speed, course,
 * or altitude — so [GpsFix.speedMps] stays 0 for every fix on iOS. Distance/point-count are exact
 * (they come from Haversine over real displacement); `avgSpeedMps`/`maxSpeedMps` stay 0 until
 * `GeoPoint` gains a speed field (kmp-toolkit change, out of scope here — see PROGRESS.md).
 *
 * iOS-specific details (Info.plist `location` bg mode, NSLocationAlwaysUsageDescription, background
 * session limits) are documented in .ralph/PROGRESS.md.
 */
class IosTrackingController(
    private val locationTracker: LocationTracker,
    private val statePublisher: TrackingStatePublisher,
    private val trackRepository: SavedTrackRepository? = null,
    private val locationRepository: LocationRepository? = null,
) : TrackingController {
    private var activeToken: String? = null

    // Per-session fix-collection scope: created in start(), cancelled in stop()/pause is a no-op on it.
    private var scope: CoroutineScope? = null

    // Survives stop() so the finalize (DB read + SavedTrack update) isn't cancelled by the same
    // stop() call that tears down the fix-collection scope above.
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var processor: LocationProcessor? = null
    private var locationBatcher: LocationBatcher? = null
    private var startTime: Long = 0L
    private var isPaused: Boolean = false
    private var everPaused: Boolean = false

    // P-C.2: set to true when the iOS app was relaunched by CLLocationManager
    // (significant-change monitoring) after an OS-kill. Swift/AppDelegate must call
    // markSystemRelaunch() before start() if it detects a launchOptions[locationKey].
    private var pendingSystemRelaunch = false

    fun markSystemRelaunch() {
        pendingSystemRelaunch = true
    }

    override fun start(token: String) {
        if (activeToken != null) return
        val wasSystemRelaunch = pendingSystemRelaunch
        pendingSystemRelaunch = false
        activeToken = token
        startTime = nowMs()
        isPaused = false
        everPaused = false
        processor = LocationProcessor()
        locationBatcher = locationRepository?.let { LocationBatcher(it, now = ::nowMs) }

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = sessionScope
        locationTracker.start()
        statePublisher.update {
            TrackingSnapshot(state = TrackingState.LIVE_TRACKING, token = token)
        }
        // P-C.2: if the app was relaunched by the OS (significant-change), mark the flag.
        if (wasSystemRelaunch && trackRepository != null) {
            sessionScope.launch {
                SystemRecoveryDetector(trackRepository).handleIfSystemRecovery(token, true)
            }
        }

        sessionScope.launch {
            locationTracker.updates.collect { point ->
                val proc = processor ?: return@collect
                val result = proc.process(point.toGpsFix(), isPaused) ?: return@collect // jitter-suppressed
                locationBatcher?.add(result.location.copy(token = token))
                val stats = proc.stats()
                val quality =
                    TrackingQualityScorer.score(
                        QualityInputs(
                            isMock = result.isMock,
                            isPowerSaver = false,
                            accuracyM = result.location.accuracy,
                            isStable = !result.isAbnormal,
                        ),
                    )
                statePublisher.update { snap ->
                    snap.copy(
                        state = if (isPaused) TrackingState.PAUSED else TrackingState.LIVE_TRACKING,
                        token = token,
                        distanceMeters = stats.cleanedDistanceM,
                        durationMs = nowMs() - startTime,
                        avgSpeedMps = stats.avgSpeedMps,
                        maxSpeedMps = stats.maxSpeedMps,
                        totalPoints = stats.totalPoints,
                        qualityScore = quality,
                        spikeDistanceM = stats.abnormalDistanceM,
                        isGpsAvailable = true,
                    )
                }
            }
        }
    }

    override fun pause(token: String) {
        if (activeToken != token) return
        isPaused = true
        everPaused = true
        locationTracker.stop()
        // Data-loss guardrail (mirrors the Android service): flush buffered points now rather than
        // waiting for the next full-batch/30s trigger, since pause can precede backgrounding.
        controllerScope.launch { locationBatcher?.flush() }
        statePublisher.update { it.copy(state = TrackingState.PAUSED) }
    }

    override fun resume(token: String) {
        if (activeToken != token) return
        isPaused = false
        // P-A.3: reset Kalman on resume so stale pre-pause filter state doesn't distort the new segment.
        processor?.resetKalman()
        locationTracker.start()
        statePublisher.update { it.copy(state = TrackingState.LIVE_TRACKING) }
    }

    override fun stop(token: String) {
        if (activeToken != token) return
        activeToken = null
        val proc = processor
        val batcher = locationBatcher
        val repo = locationRepository
        val trackRepo = trackRepository
        val start = startTime
        val wasEverPaused = everPaused
        scope?.cancel()
        scope = null
        locationTracker.stop()
        statePublisher.update { TrackingSnapshot(state = TrackingState.COMPLETED) }

        if (proc != null) {
            controllerScope.launch {
                batcher?.flush()
                val stats = proc.stats()
                val endTime = nowMs()
                // P-A.2 parity: recompute the authoritative cleaned distance from persisted DB
                // points (consecutive-Haversine excluding isAbnormal/isMock/isPaused), falling back
                // to in-memory stats when nothing made it to the DB yet (very short trip / no repo).
                val dbPoints = repo?.getForToken(token) ?: emptyList()
                val finalCleanedDistanceM =
                    if (dbPoints.isNotEmpty()) DistanceCalculator.computeCleanedDistance(dbPoints) else stats.cleanedDistanceM
                val finalTotalPoints = if (dbPoints.isNotEmpty()) dbPoints.size.toLong() else stats.totalPoints.toLong()
                trackRepo?.getByRouteId(token)?.let { t ->
                    trackRepo.update(
                        t.copy(
                            isCompleted = true,
                            endTime = endTime,
                            distance = finalCleanedDistanceM,
                            duration = endTime - start,
                            originalDistance = stats.originalDistanceM,
                            cleanedDistance = finalCleanedDistanceM,
                            abnormalDistance = stats.abnormalDistanceM,
                            mockDistance = stats.mockDistanceM,
                            smartDistanceFinal = finalCleanedDistanceM,
                            totalLocationPoints = finalTotalPoints,
                            avgSpeed = stats.avgSpeedMps,
                            maxSpeed = stats.maxSpeedMps,
                            wasMockLocationUsed = stats.mockDistanceM > 0.0,
                            wasEverPaused = t.wasEverPaused || wasEverPaused,
                        ),
                    )
                }
            }
        }
        processor = null
        locationBatcher = null
    }
}

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * `GeoPoint` (kmp-toolkit) carries lat/lng/accuracy/timestamp only — no speed/course/altitude (see
 * the class doc). [GpsFix.speedMps]/[GpsFix.bearingDeg]/[GpsFix.altitudeM] stay at their 0 defaults;
 * distance accumulation is displacement-driven (Haversine between consecutive fixes) so it is
 * unaffected, but the processor's speed accumulator (fed by [GpsFix.speedMps]) never advances.
 */
private fun GeoPoint.toGpsFix(): GpsFix =
    GpsFix(
        lat = latitude,
        lng = longitude,
        timeMs = timestampMillis,
        accuracyM = accuracyMeters,
        provider = "ios",
    )
