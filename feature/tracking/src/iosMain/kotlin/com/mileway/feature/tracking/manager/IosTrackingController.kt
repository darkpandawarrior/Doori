package com.mileway.feature.tracking.manager

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.service.LocationBatcher
import com.mileway.feature.tracking.service.LocationTrackingConstants
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
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
 * `GeoPoint` (kmp-toolkit `:app-shell`) carries speed/course/altitude straight from `CLLocation`, so
 * [GpsFix.speedMps]/[GpsFix.bearingDeg]/[GpsFix.altitudeM] are real per-fix values on iOS (see
 * [toGpsFix]) — `avgSpeedMps`/`maxSpeedMps` in the published snapshot are no longer stuck at 0.
 *
 * A staleness watchdog ([startWatchdog]) re-invokes [LocationTracker.start] if no fix has arrived
 * within [STALE_FIX_TIMEOUT_MS] while tracking is active and unpaused — mirrors the RN lesson that a
 * background location callback can silently die without the OS surfacing an error. It runs only
 * between [start]/[resume] and [pause]/[stop] (see those methods).
 *
 * iOS-specific details (Info.plist `location` bg mode, NSLocationAlwaysUsageDescription, background
 * session limits) are documented in .ralph/PROGRESS.md.
 */
class IosTrackingController(
    private val locationTracker: LocationTracker,
    private val statePublisher: TrackingStatePublisher,
    private val trackRepository: SavedTrackRepository? = null,
    private val locationRepository: LocationRepository? = null,
    // Gap-fix (see class doc): mirrors the Android service's currentTrackRepository/pluginRegistry/
    // demoSettings/configManager injection so CurrentTrackData and the user-tunable knobs below are
    // no longer iOS-only dead wiring.
    private val currentTrackRepository: CurrentTrackRepository? = null,
    private val configManager: TrackingConfigManager? = null,
    private val pluginRegistry: PluginRegistry? = null,
    private val demoSettings: DemoSettingsRepository? = null,
) : TrackingController {
    private var activeToken: String? = null

    // Per-session fix-collection scope: created in start(), cancelled in stop()/pause is a no-op on it.
    private var scope: CoroutineScope? = null

    // Survives stop() so the finalize (DB read + SavedTrack update) isn't cancelled by the same
    // stop() call that tears down the fix-collection scope above.
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // P10.1/Wave-2 parity: hot-reloaded Track Miles knobs + AbnormalDetectionConfig, mirrored from
    // PluginRegistry/DemoSettingsRepository/TrackingConfigManager exactly like the Android service's
    // onCreate collectors (see LocationTrackingService). Read at the next start() — fixed for the
    // running trip, same rule as Android.
    @Volatile
    private var enableKalman: Boolean = false

    @Volatile
    private var maxAccuracyM: Double = LocationTrackingConstants.MAX_ACCURACY_THRESHOLD_M

    @Volatile
    private var minDisplacementFloorM: Double = 0.0

    @Volatile
    private var abnormalDetectionConfig: AbnormalDetectionConfig = AbnormalDetectionConfig.DEFAULT

    init {
        // controllerScope (not the per-session `scope`) so these keep hot-reloading across trips —
        // this class is a Koin single, constructed once for the app's lifetime.
        demoSettings?.let { ds -> controllerScope.launch { ds.settings.collect { enableKalman = it.enableKalman } } }
        pluginRegistry?.let { pr ->
            controllerScope.launch {
                pr.observeValue("track_min_accuracy_m").collect {
                    maxAccuracyM =
                        ((it as? PluginValue.IntVal)?.value ?: LocationTrackingConstants.MAX_ACCURACY_THRESHOLD_M.toInt()).toDouble()
                }
            }
            controllerScope.launch {
                pr.observeValue("track_min_displacement_m").collect {
                    minDisplacementFloorM = ((it as? PluginValue.IntVal)?.value ?: 0).toDouble()
                }
            }
        }
        configManager?.let { cm -> controllerScope.launch { cm.abnormalDetectionConfig.collect { abnormalDetectionConfig = it } } }
    }

    private var processor: LocationProcessor? = null
    private var locationBatcher: LocationBatcher? = null

    // Gap-fix: the saved_tracks row fetched once per trip so the per-fix loop can write live
    // distance/duration without a DB read on every fix (see start()/the fix-collection loop).
    private var liveTrack: SavedTrack? = null
    private var startTime: Long = 0L
    private var isPaused: Boolean = false
    private var everPaused: Boolean = false

    // P-C.2: set to true when the iOS app was relaunched by CLLocationManager
    // (significant-change monitoring) after an OS-kill. Swift/AppDelegate must call
    // markSystemRelaunch() before start() if it detects a launchOptions[locationKey].
    private var pendingSystemRelaunch = false

    // Staleness watchdog: last time a GeoPoint arrived, and the coroutine Job checking it.
    private var lastFixAtMs: Long = 0L
    private var watchdogJob: Job? = null

    fun markSystemRelaunch() {
        pendingSystemRelaunch = true
    }

    /** (Re)starts the staleness watchdog on [scope]; cancels any previous instance first. */
    private fun startWatchdog(scope: CoroutineScope) {
        watchdogJob?.cancel()
        watchdogJob =
            scope.launch {
                while (true) {
                    delay(WATCHDOG_CHECK_INTERVAL_MS)
                    if (!isPaused && nowMs() - lastFixAtMs > STALE_FIX_TIMEOUT_MS) {
                        // Self-heal: the OS may have silently stopped delivering fixes (backgrounded
                        // app, killed location daemon, etc.) without CoreLocation raising an error.
                        locationTracker.start()
                    }
                }
            }
    }

    override fun start(token: String) {
        if (activeToken != null) return
        val wasSystemRelaunch = pendingSystemRelaunch
        pendingSystemRelaunch = false
        activeToken = token
        startTime = nowMs()
        isPaused = false
        everPaused = false
        processor =
            LocationProcessor(
                enableKalman = enableKalman,
                maxAccuracyThreshold = maxAccuracyM,
                minDisplacementFloorM = minDisplacementFloorM,
                abnormalConfig = abnormalDetectionConfig,
            )
        locationBatcher = locationRepository?.let { LocationBatcher(it, now = ::nowMs) }
        liveTrack = null

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = sessionScope
        lastFixAtMs = startTime
        locationTracker.start()
        startWatchdog(sessionScope)
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
            // Gap-fix: cache the saved_tracks row for the per-fix live-distance write below, and mark
            // CurrentTrackData tracking (mirrors the Android service's persistSession) — this is what
            // makes TrackingRelaunchBridge's isTracking gate reachable and feeds the session-derived
            // UI (LiveTrackViewModel/WatchFacade). Sequenced before collect() so the first fixes below
            // don't race a still-null liveTrack/un-started CurrentTrackData.
            trackRepository?.let { repo -> liveTrack = repo.getByRouteId(token) }
            currentTrackRepository?.let { repo ->
                val current = repo.getCurrentTrackDataRawAsync().getOrNull() ?: CurrentTrackData.empty()
                // Zero the stat fields rather than carrying over a previous trip's leftovers under
                // this new token — the first accepted fix below overwrites them for real within
                // seconds anyway, so this only avoids a stale flash of the last trip's numbers.
                repo.startSession(
                    current.copy(
                        token = token,
                        isTracking = true,
                        isPaused = false,
                        startTime = startTime,
                        distance = 0.0,
                        speed = 0.0,
                        avgSpeed = 0.0,
                        maxSpeed = 0.0,
                        totalLocationPoints = 0L,
                        startedAtTimestamp = if (current.startedAtTimestamp == 0L) startTime else current.startedAtTimestamp,
                    ),
                )
            }

            locationTracker.updates.collect { point ->
                lastFixAtMs = nowMs()
                val proc = processor ?: return@collect
                val result = proc.process(point.toGpsFix(), isPaused) ?: return@collect // jitter-suppressed
                locationBatcher?.add(result.location.copy(token = token))
                val stats = proc.stats()
                val durationMs = nowMs() - startTime
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
                        durationMs = durationMs,
                        avgSpeedMps = stats.avgSpeedMps,
                        maxSpeedMps = stats.maxSpeedMps,
                        totalPoints = stats.totalPoints,
                        qualityScore = quality,
                        spikeDistanceM = stats.abnormalDistanceM,
                        isGpsAvailable = true,
                    )
                }
                // Gap-fix: live distance into the saved_tracks row — mirrors Android's
                // savedTrackDao.updateTrackLiveData per-fix cadence (LocationTrackingService:516) so
                // the hero distance advances during tracking instead of reading 0.00 until stop().
                liveTrack?.let { t ->
                    val updated = t.copy(distance = stats.cleanedDistanceM, duration = durationMs)
                    liveTrack = updated
                    trackRepository?.update(updated)
                }
                // Gap-fix: mirrors persistSession's per-fix CurrentTrackData write (same cadence).
                currentTrackRepository?.updateBatchedDistanceAndPoints(
                    token = token,
                    distanceMeters = stats.cleanedDistanceM,
                    speed = result.location.speed.toDouble(),
                    avgSpeed = stats.avgSpeedMps,
                    totalPoints = stats.totalPoints.toLong(),
                    unsyncedPoints = stats.totalPoints.toLong(),
                )
            }
        }
    }

    override fun pause(token: String) {
        if (activeToken != token) return
        isPaused = true
        everPaused = true
        watchdogJob?.cancel()
        watchdogJob = null
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
        lastFixAtMs = nowMs()
        locationTracker.start()
        scope?.let { startWatchdog(it) }
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
        watchdogJob = null
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
                // Gap-fix: mirrors clearSessionTracking — flips isTracking off so LiveTrackViewModel
                // and TrackingRelaunchBridge stop treating this token as a live session.
                currentTrackRepository?.let { ctRepo ->
                    val current = ctRepo.getCurrentTrackDataRawAsync().getOrNull()
                    if (current != null) {
                        ctRepo.startSession(current.copy(isTracking = false, isPaused = false, endTime = endTime))
                    }
                }
            }
        }
        processor = null
        locationBatcher = null
        liveTrack = null
    }
}

// How often the watchdog checks for a stale fix, and how long without a fix counts as stale.
// 90s is ~20-45x the typical CoreLocation update cadence — comfortably past normal jitter/backoff,
// short enough that a genuinely dead tracker gets restarted well before a trip is ruined.
private const val WATCHDOG_CHECK_INTERVAL_MS = 15_000L
private const val STALE_FIX_TIMEOUT_MS = 90_000L

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * `GeoPoint` (kmp-toolkit) now carries real speed/course/altitude from `CLLocation` (negative
 * speed/course means "invalid" per CoreLocation's convention — guarded here to the [GpsFix] 0
 * defaults rather than propagating a negative speed/bearing into the distance/quality pipeline).
 */
private fun GeoPoint.toGpsFix(): GpsFix =
    GpsFix(
        lat = latitude,
        lng = longitude,
        timeMs = timestampMillis,
        speedMps = if (speedMetersPerSecond < 0f) 0f else speedMetersPerSecond,
        accuracyM = accuracyMeters,
        bearingDeg = if (courseDegrees < 0.0) 0f else courseDegrees.toFloat(),
        altitudeM = altitudeMeters,
        provider = "ios",
    )
