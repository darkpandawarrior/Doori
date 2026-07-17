package com.mileway.feature.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.display.InMemorySnapshotPublisher
import com.mileway.core.data.model.display.SurfaceSnapshotProducer
import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.watch.SnapshotCache
import com.mileway.core.data.watch.toWatchPayload
import com.mileway.core.platform.MotionFusion
import com.mileway.core.platform.MotionReading
import com.mileway.core.platform.Vector3
import com.mileway.feature.tracking.TrackMilesActivity
import com.mileway.feature.tracking.manager.AndroidDeviceRamSource
import com.mileway.feature.tracking.manager.DeviceTierManager
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.FusedLocationSource
import com.mileway.feature.tracking.service.location.GpsFix
import com.mileway.feature.tracking.service.location.LocationProcessor
import com.mileway.feature.tracking.service.location.LocationSource
import com.mileway.feature.tracking.service.location.RecognizedActivity
import com.mileway.feature.tracking.service.location.SimulatedLocationSource
import com.mileway.feature.tracking.service.location.TrackStats
import com.mileway.feature.tracking.service.location.TrackingSensorMonitor
import com.mileway.feature.tracking.service.motion.ImuAnalyzer
import com.siddharth.kmp.location.DynamicIntervalCalculator
import com.siddharth.kmp.location.IntervalInputs
import com.siddharth.kmp.location.QualityInputs
import com.siddharth.kmp.location.TrackingQualityScorer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Advanced foreground location-tracking service.
 *
 * Pulls fixes from a [LocationSource] (real fused GPS in production, a simulated route in the
 * offline demo), captures IMU context via [TrackingSensorMonitor], and runs each fix through
 * [LocationProcessor] for Haversine distance accumulation, jitter suppression, and mock/spike
 * detection. Persists points to Room (`locations`), keeps the `saved_tracks` row + the
 * `current_track_session` DataStore live for the UI, and logs diagnostic [HardwareEvent]s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackingService : Service() {
    private val locationDao: LocationDao by inject()
    private val savedTrackDao: SavedTrackDao by inject()
    private val sessionStore: CurrentTrackDataStore by inject()
    private val demoSettings: DemoSettingsRepository by inject()
    private val pluginRegistry: com.mileway.core.data.plugin.PluginRegistry by inject()
    private val configManager: com.mileway.feature.tracking.manager.TrackingConfigManager by inject()
    private val locationRepository: LocationRepository by inject()
    private val currentTrackRepository: CurrentTrackRepository by inject()
    private val hardwareEventRepository: HardwareEventRepository by inject()

    // Wave-2: buffers point writes (10pt/30s) instead of one Room insert per fix.
    private val locationBatcher: LocationBatcher by lazy {
        LocationBatcher(locationRepository, now = System::currentTimeMillis)
    }

    // Wave-2: periodic (120s) in-memory-vs-DB drift check, piggybacked on the per-fix path.
    private val driftReconciler: DriftReconciler by lazy { DriftReconciler(now = System::currentTimeMillis) }

    // Wave-2 SmartEventLogger (parity §2.8): throttles noisy per-fix hardware events
    // (MOCK_LOCATION/ABNORMAL_LOCATION) before they hit Room; critical lifecycle events pass through.
    private val smartEventLogger: SmartEventLogger by lazy {
        SmartEventLogger(hardwareEventRepository, EventThrottler(now = System::currentTimeMillis))
    }

    // P-C.3: consume the shutdown flag once per boot.
    private val shutdownFlagPolicy: ShutdownFlagPolicy by lazy {
        val prefs = getSharedPreferences(AndroidShutdownFlagStore.PREFS_NAME, Context.MODE_PRIVATE)
        ShutdownFlagPolicy(AndroidShutdownFlagStore(prefs), SavedTrackRepository(savedTrackDao))
    }

    // G6: persisted opt-in for the live Kalman smoother. Collected continuously so the
    // customization toggle takes effect from the next trip start (the smoother is fixed for the
    // lifetime of a running trip; we don't swap algorithms mid-journey).
    @Volatile
    private var enableKalman: Boolean = false

    // P10.1: persisted Track Miles knobs, mirrored from the PluginRegistry the same way as
    // enableKalman — read at the next trip start, fixed for the running trip. Defaults reproduce
    // today's math exactly: the shipped accuracy gate, no displacement floor, no interval floor,
    // fused (not raw-GPS) provider.
    @Volatile
    private var maxAccuracyM: Double = LocationTrackingConstants.MAX_ACCURACY_THRESHOLD_M

    @Volatile
    private var minDisplacementFloorM: Double = 0.0

    @Volatile
    private var intervalFloorMs: Long = 0L

    @Volatile
    private var forceGpsOnly: Boolean = false

    // P10.5: floating-bubble + overlay master, mirrored from the PluginRegistry. The bubble shows
    // during a trip only when the overlay master is on AND the bubble toggle is on (both default so
    // that no bubble appears unless the user opts in — unchanged shipped behavior). Read at trip
    // start (bubble lifecycle is bound to the running trip).
    @Volatile
    private var showBubble: Boolean = false

    @Volatile
    private var showTrackingOverlay: Boolean = true

    // Wave-2 AbnormalDetectionConfig: hot-reloaded from TrackingConfigManager (debug settings
    // today, server config later). Same "fixed for the running trip" rule as enableKalman above.
    @Volatile
    private var abnormalDetectionConfig: com.mileway.feature.tracking.manager.AbnormalDetectionConfig =
        com.mileway.feature.tracking.manager.AbnormalDetectionConfig.DEFAULT

    // C.2b: live telemetry the ViewModel observes via TrackingServiceApi.
    private val statePublisher: TrackingStatePublisher by inject()

    // L.1: glanceable-surface snapshot channel; refreshed when a trip completes.
    private val snapshotPublisher: InMemorySnapshotPublisher by inject()

    // P6.1: cross-process cache the widget/extension process reads without touching Room.
    private val snapshotCache: SnapshotCache by inject()

    // C.2d: notification throttle — last type/time published, to drop sub-throttle same-type updates.
    private var lastNotificationType: TrackingNotificationType? = null
    private var lastNotificationAtMs: Long = 0L

    // C.2g: timestamp of the last resume; the grace window runs for RESUME_GRACE_MS after it (0 = closed).
    private var resumeAtMs: Long = 0L

    // O.3: running gravity estimate for the per-fix IMU stillness check (sensor fusion).
    private var motionGravity = Vector3(0f, 0f, 0f)

    // O.2: activity recognition, fused into stillness alongside the IMU. Latest value cached so the
    // per-fix path reads it cheaply; the stream stops when [scope] is cancelled in onDestroy.
    // PLAN_V37 Phase 1: Koin-bound (gms -> GmsActivityRecognizer, noGms -> HeuristicActivityRecognizer,
    // both in :app's flavor source sets) instead of constructed inline — this class no longer imports
    // GmsActivityRecognizer/Play Services directly.
    private val activityRecognizer: ActivityRecognizer by inject()

    @Volatile
    private var recognizedActivity: RecognizedActivity = RecognizedActivity.UNKNOWN

    // Wave-2 DeviceTierManager: resolved once per process (RAM tier doesn't change at runtime),
    // then reused as the interval multiplier for every fix.
    private val tierIntervalMultiplier: Double by lazy {
        DeviceTierManager.intervalMultiplier(DeviceTierManager.tierFor(AndroidDeviceRamSource(this).totalRamBytes()))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Serialize DB writes so live-stat updates apply in order.
    private val dbDispatcher = Dispatchers.IO.limitedParallelism(1)

    private lateinit var sensorMonitor: TrackingSensorMonitor
    private var source: LocationSource? = null
    private var processor: LocationProcessor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var activeToken: String? = null
    private var startTime: Long = 0L
    private var isPaused: Boolean = false
    private var startCoordsWritten = false

    // A.2: watchdog that falls back to the simulated drive source if the real GPS provider never
    // delivers a fix (no hardware fix / permission edge / indoors), so the UI never stalls on
    // "Waiting for location…". Cancelled the moment the first real fix lands.
    private var firstFixSeen = false
    private var fixWatchdogJob: Job? = null

    companion object {
        const val EXTRA_TOKEN = "token"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_RESTORE = "action_restore"

        // P-D.1: opens device location settings from the GPS-disabled notification action.
        const val ACTION_FIX_GPS = "action_fix_gps"
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001

        // C.2d: minimum gap between same-type notification updates (live ACTIVE fires per fix).
        const val NOTIFICATION_THROTTLE_MS = 2_000L

        // C.2g: how long after a resume to suppress spike rejection / auto-discard.
        const val RESUME_GRACE_MS = 5_000L

        /** Demo flag: drive the pipeline from a simulated route (no GPS hardware required). */
        const val SIMULATE_LOCATION = true

        /**
         * A.2: if the real GPS provider hasn't produced a single fix within this window, fall back
         * to the deterministic simulated drive so distance/duration/speed advance regardless of
         * hardware. Inert when [SIMULATE_LOCATION] is on (already simulated from the start).
         */
        const val FIRST_FIX_TIMEOUT_MS = 6_000L
    }

    override fun onCreate() {
        super.onCreate()
        sensorMonitor = TrackingSensorMonitor(this)
        createNotificationChannel()
        // G6: keep the Kalman opt-in mirrored from persisted settings.
        scope.launch { demoSettings.settings.collect { enableKalman = it.enableKalman } }
        // P10.1: mirror the persisted Track Miles knobs from the PluginRegistry (same "fixed for
        // the running trip" rule as enableKalman — read at the next startTracking()).
        scope.launch {
            pluginRegistry.observeValue("track_min_accuracy_m").collect {
                maxAccuracyM =
                    (
                        (it as? com.mileway.core.data.plugin.PluginValue.IntVal)?.value
                            ?: LocationTrackingConstants.MAX_ACCURACY_THRESHOLD_M.toInt()
                    ).toDouble()
            }
        }
        scope.launch {
            pluginRegistry.observeValue("track_min_displacement_m").collect {
                minDisplacementFloorM = ((it as? com.mileway.core.data.plugin.PluginValue.IntVal)?.value ?: 0).toDouble()
            }
        }
        scope.launch {
            pluginRegistry.observeValue("track_location_interval_s").collect {
                intervalFloorMs = ((it as? com.mileway.core.data.plugin.PluginValue.IntVal)?.value ?: 0).toLong() * 1_000L
            }
        }
        scope.launch { pluginRegistry.observe("track_force_gps_only").collect { forceGpsOnly = it } }
        // P10.5: mirror the bubble + overlay-master toggles (read at trip start).
        scope.launch { pluginRegistry.observe("show_tracking_bubble").collect { showBubble = it } }
        scope.launch { pluginRegistry.observe("track_show_overlay").collect { showTrackingOverlay = it } }
        // Wave-2 AbnormalDetectionConfig: mirror the hot-reload Flow into a plain var the
        // per-fix path reads (process() is not suspend and runs off the main thread).
        scope.launch { configManager.abnormalDetectionConfig.collect { abnormalDetectionConfig = it } }
        // O.2: mirror the recognized activity (no-op without Play Services / permission).
        scope.launch { activityRecognizer.activity.collect { recognizedActivity = it } }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // A startForegroundService() launch that doesn't reach startForeground() within the
        // ANR window kills the whole process (ForegroundServiceDidNotStartInTimeException),
        // so promote to foreground before ANY other work, especially before the suspendable
        // session-restore read below.
        if (!enterForeground()) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_START -> {
                val token = intent.getStringExtra(EXTRA_TOKEN)
                if (token.isNullOrEmpty()) {
                    leaveForegroundAndStop()
                    return START_NOT_STICKY
                }
                startTracking(token)
            }
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopAndFinalize()
            // P-D.1: GPS-disabled notification action — open device location settings.
            ACTION_FIX_GPS ->
                startActivity(
                    Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            // Boot/update restore, and the system redelivering a null intent when it
            // restarts a killed sticky service: both re-derive state from the persisted
            // session rather than trusting the intent.
            ACTION_RESTORE, null -> restoreSession(intent?.getStringExtra(EXTRA_TOKEN))
            else -> leaveForegroundAndStop()
        }
        return START_STICKY
    }

    /** Calls startForeground defensively. Returns false (and stops self) on failure. */
    private fun enterForeground(): Boolean =
        try {
            startForeground(NOTIFICATION_ID, buildNotification(TrackingNotificationMapper.fromSnapshot(TrackingSnapshot())))
            true
        } catch (e: Exception) {
            // e.g. missing FGS location permission on Android 14+, or a background start the
            // OS disallows (ForegroundServiceStartNotAllowedException). Don't crash the app.
            Napier.w("startForeground failed", e, tag = "LocationTrackingService")
            stopSelf()
            false
        }

    private fun leaveForegroundAndStop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Resume an interrupted session (after reboot, app update, or sticky-service restart).
     * The DataStore read suspends, which is safe here because we are already foreground.
     */
    private fun restoreSession(intentToken: String?) {
        if (activeToken != null) return // already tracking: duplicate restore, ignore
        val isSystemRelaunch = intentToken == null // null intent = OS-killed sticky restart (L2)
        scope.launch {
            val session = sessionStore.currentTrackFlow.first()
            val token = intentToken?.takeIf { it.isNotEmpty() } ?: session.token
            val resumable = session.isTracking && token.isNotEmpty() && token == session.token
            // P-A.4: reconcile DataStore point-counter against DB before seeding the processor
            // so resume-from-kill sessions start with accurate totalPoints.
            val reconciledSession =
                if (resumable) {
                    val dbCount = locationDao.countLocationsByToken(token).toLong()
                    val result = CounterReconcilePolicy.reconcile(session.totalLocationPoints, dbCount)
                    if (result.isDiverged) {
                        sessionStore.updateLocationCount(token, result.dbCount, result.dbCount)
                        session.copy(totalLocationPoints = result.dbCount)
                    } else {
                        session
                    }
                } else {
                    session
                }
            if (resumable && isSystemRelaunch) {
                // P-C.2: OS redelivered a null intent → FGS was terminated. Write the flag and
                // log the event so the quality scorer and UI know about the gap.
                savedTrackDao.markFgTerminated(token)
                logEventSuspend(token, EventType.SYSTEM_RECOVERY, "FGS terminated + relaunched by OS")
            }
            if (resumable) {
                // P-C.3: consume the shutdown flag if a previous boot wrote it.
                val wasShutDown = shutdownFlagPolicy.consumeAndMark(token)
                if (wasShutDown) {
                    logEventSuspend(token, EventType.PHONE_RESTART, "Phone restarted after shutdown")
                }
            }
            withContext(Dispatchers.Main) {
                if (resumable) {
                    startTracking(token, resumeFrom = reconciledSession)
                } else {
                    leaveForegroundAndStop()
                }
            }
        }
    }

    private fun startTracking(
        token: String,
        resumeFrom: CurrentTrackData? = null,
    ) {
        activeToken = token
        startTime = resumeFrom?.startTime?.takeIf { it > 0L } ?: System.currentTimeMillis()
        isPaused = resumeFrom?.isPaused ?: false
        startCoordsWritten = resumeFrom != null &&
            (resumeFrom.startLatitude != 0.0 || resumeFrom.startLongitude != 0.0)
        processor =
            LocationProcessor(
                deviceModel = Build.MODEL ?: "",
                appVersionName = appVersionName(),
                initialStats = resumeFrom?.let { seedStats(it) },
                // G6: route live fixes through the Kalman smoother when the user has opted in.
                enableKalman = enableKalman,
                // P10.1: user-set accuracy gate (Track Miles setting), default = the shipped
                // MAX_ACCURACY_THRESHOLD_M so unset behavior matches configManager's default.
                maxAccuracyThreshold = maxAccuracyM,
                // P10.1: user-set global min-displacement floor over the per-band jitter gate.
                minDisplacementFloorM = minDisplacementFloorM,
                // Wave-2 AbnormalDetectionConfig: latest hot-reloaded thresholds, fixed for this trip.
                abnormalConfig = abnormalDetectionConfig,
            )

        acquireWakeLock()
        sensorMonitor.start()
        logEvent(
            token,
            if (resumeFrom != null) EventType.TRACKING_RESUMED else EventType.TRACKING_STARTED,
            if (resumeFrom != null) "Tracking Restored After Restart" else "Tracking Started",
        )

        statePublisher.update {
            TrackingSnapshot(
                state = if (isPaused) TrackingState.PAUSED else TrackingState.LIVE_TRACKING,
                token = token,
                lastEvent = if (resumeFrom != null) EventType.TRACKING_RESUMED else EventType.TRACKING_STARTED,
            )
        }

        firstFixSeen = false
        // P10.1: forceGpsOnly (Track Miles setting) selects the raw-GPS provider inside FusedLocationSource.
        source = if (SIMULATE_LOCATION) SimulatedLocationSource() else FusedLocationSource(this, forceGpsOnly = forceGpsOnly)
        source?.start { fix -> onFix(token, fix) }
        // A.2: arm the no-fix watchdog for the real-GPS path only.
        if (!SIMULATE_LOCATION) armFirstFixWatchdog(token)
        // P10.5: show the floating bubble for the running trip when the overlay master + bubble
        // toggles are both on (FloatingBubbleService itself no-ops without the overlay permission).
        if (showBubble && showTrackingOverlay) FloatingBubbleService.start(this)
    }

    /**
     * If no real fix lands within [FIRST_FIX_TIMEOUT_MS], swap the live source for a
     * [SimulatedLocationSource] so the tracking UI advances instead of stalling on
     * "Waiting for location…". Self-cancels as soon as the first real fix arrives.
     */
    private fun armFirstFixWatchdog(token: String) {
        fixWatchdogJob?.cancel()
        fixWatchdogJob =
            scope.launch {
                delay(FIRST_FIX_TIMEOUT_MS)
                if (firstFixSeen || activeToken != token) return@launch
                withContext(Dispatchers.Main) {
                    if (firstFixSeen || activeToken != token) return@withContext
                    source?.stop()
                    source = SimulatedLocationSource()
                    source?.start { fix -> onFix(token, fix) }
                    logEvent(token, EventType.TRACKING_STARTED, "No GPS fix — using simulated drive")
                }
            }
    }

    /**
     * Rebuild processor accumulators from the persisted session so distance/duration carry
     * over a restart instead of resetting to zero. Only the cleaned totals are persisted
     * live, so the abnormal/mock buckets restart at zero for the resumed segment.
     */
    private fun seedStats(s: CurrentTrackData) =
        TrackStats(
            totalPoints = s.totalLocationPoints.toInt(),
            originalDistanceM = s.distance,
            cleanedDistanceM = s.distance,
            abnormalDistanceM = 0.0,
            mockDistanceM = 0.0,
            avgSpeedMps = s.avgSpeed,
            maxSpeedMps = s.maxSpeed,
        )

    private fun onFix(
        token: String,
        fix: GpsFix,
    ) {
        // A.2: the first fix (real or simulated) disarms the no-fix watchdog.
        if (!firstFixSeen) {
            firstFixSeen = true
            fixWatchdogJob?.cancel()
        }
        val proc = processor ?: return
        // C.2g: within the post-resume grace window, accept a large jump (the user moved while paused)
        // instead of rejecting it as a teleport spike.
        val inResumeGrace = resumeAtMs > 0L && System.currentTimeMillis() - resumeAtMs < RESUME_GRACE_MS
        val sensors = sensorMonitor.snapshot
        // O.3: fuse the IMU — when the accelerometer says the device is physically still, strengthen GPS
        // jitter suppression. Only when real sensor data is present (zeros in the simulated/no-sensor path
        // leave it off, so the pipeline behaves exactly as before).
        val hasImu = sensors.accelX != 0f || sensors.accelY != 0f || sensors.accelZ != 0f
        val motionStill =
            if (hasImu) {
                val reading =
                    MotionReading(
                        accelX = sensors.accelX,
                        accelY = sensors.accelY,
                        accelZ = sensors.accelZ,
                        timestampMillis = fix.timeMs,
                    )
                motionGravity = MotionFusion.updateGravity(motionGravity, reading)
                !MotionFusion.isMoving(reading, motionGravity)
            } else {
                false
            } || recognizedActivity == RecognizedActivity.STILL // O.2: activity recognition also signals stillness
        // Wave-2 IMU polish: harsh-accel/gyro-spin read off the raw snapshot, plus pausing gyro
        // consumption while confirmed stationary (battery win — resumed the instant motion returns).
        val imuAnalysis = ImuAnalyzer.analyze(sensors, motionStill)
        sensorMonitor.setStationary(motionStill)
        val result =
            proc.process(
                fix,
                isPaused,
                sensors,
                suppressSpike = inResumeGrace,
                motionStill = motionStill,
                harshAccel = imuAnalysis.harshAccel,
            ) ?: return // jitter-suppressed
        val battery = batteryPercent()
        val row = result.location.copy(token = token, batteryPercentage = battery)
        val stats = proc.stats()
        val durationMs = System.currentTimeMillis() - startTime

        scope.launch(dbDispatcher) {
            locationBatcher.add(row)
            savedTrackDao.updateTrackLiveData(token, stats.cleanedDistanceM, durationMs)
            if (!startCoordsWritten && proc.firstLat != null) {
                startCoordsWritten = true
                savedTrackDao.getSavedTrackById(token)?.let { t ->
                    savedTrackDao.updateSavedTrack(
                        t.copy(startLatitude = proc.firstLat!!, startLongitude = proc.firstLng!!),
                    )
                }
            }
            persistSession(token, fix, stats, battery)
            // Wave-2: periodic drift check — in-memory point count vs Room's authoritative count.
            // A no-op (null) on most fixes; fires every 120s and self-corrects via DataStore.
            driftReconciler.maybeReconcile(
                inMemoryCount = stats.totalPoints.toLong(),
                dbCount = locationDao.countLocationsByToken(token).toLong(),
            )?.let { result ->
                if (result.isDiverged) {
                    currentTrackRepository.updateLocationCount(token, result.dbCount, result.dbCount)
                }
            }
        }

        if (result.isMock) logEvent(token, EventType.MOCK_LOCATION, "Mock Location detected", fix)
        if (result.isAbnormal) logEvent(token, EventType.ABNORMAL_LOCATION, "Abnormal Location filtered", fix)

        // C.2a: adapt the GPS request cadence to speed / battery / power-saver / session length.
        // The source re-registers only when the value moves ≥1s, so this is cheap to call per fix.
        val interval =
            DynamicIntervalCalculator.intervalMs(
                IntervalInputs(
                    speedMps = fix.speedMps.toDouble(),
                    batteryPct = battery.toInt(),
                    isCharging = isCharging(),
                    isPowerSaver = isPowerSaver(),
                    elapsedMs = durationMs,
                    tierMultiplier = tierIntervalMultiplier,
                    harshAccel = imuAnalysis.harshAccel,
                    // P10.1: user-set minimum-interval floor (Track Miles setting); 0 = no floor.
                    userFloorMs = intervalFloorMs,
                ),
            )
        source?.updateInterval(interval)

        // C.2b: score live fix quality + collect system-health flags from the signals this fix carries.
        val powerSaver = isPowerSaver()
        val systemFlags =
            // gpsDisabled stays false here: a fix just arrived, so GPS is available.
            TrackingSystemFlags(
                gpsDisabled = false,
                powerSaverOn = powerSaver,
                mockLocationDetected = result.isMock,
            )
        val qualityScore =
            TrackingQualityScorer.score(
                QualityInputs(
                    isMock = result.isMock,
                    isPowerSaver = powerSaver,
                    accuracyM = result.location.accuracy,
                    isStable = !result.isAbnormal,
                ),
            )

        // C.2b: publish live telemetry for the gauge + drive the phase-aware notification from it.
        statePublisher.update {
            it.copy(
                state = if (isPaused) TrackingState.PAUSED else TrackingState.LIVE_TRACKING,
                token = token,
                distanceMeters = stats.cleanedDistanceM,
                durationMs = durationMs,
                speedMps = fix.speedMps.toDouble(),
                avgSpeedMps = stats.avgSpeedMps,
                maxSpeedMps = stats.maxSpeedMps,
                totalPoints = stats.totalPoints,
                batteryPct = battery.toInt(),
                isCharging = isCharging(),
                currentIntervalMs = interval,
                qualityScore = qualityScore,
                spikeDistanceM = stats.abnormalDistanceM,
                isGpsAvailable = true,
                inResumeGrace = inResumeGrace,
                systemFlags = systemFlags,
                lastEvent =
                    when {
                        result.isMock -> EventType.MOCK_LOCATION
                        result.isAbnormal -> EventType.ABNORMAL_LOCATION
                        else -> null
                    },
            )
        }
        updateNotification(statePublisher.trackingState.value)
    }

    private fun setPaused(paused: Boolean) {
        // Pause/resume with no live session (service started cold), don't linger foreground.
        val token =
            activeToken ?: run {
                leaveForegroundAndStop()
                return
            }
        isPaused = paused
        // C.2g: open the resume grace window on resume, close it on pause.
        resumeAtMs = if (paused) 0L else System.currentTimeMillis()
        // P-A.3: reset Kalman on resume so stale pre-pause filter state doesn't distort the new segment.
        if (!paused) processor?.resetKalman()
        // Data-loss guardrail: pause can precede process death (backgrounded, OS reclaim), so
        // force the batcher's buffered points to Room now rather than waiting for the next
        // full-batch/30s trigger.
        if (paused) {
            scope.launch(dbDispatcher) { locationBatcher.flush() }
        }
        // P-A.4: background counter reconciliation on resume — corrects any drift while paused.
        if (!paused) {
            val resumeToken = token
            scope.launch(dbDispatcher) {
                val dbCount = locationDao.countLocationsByToken(resumeToken).toLong()
                val current = sessionStore.currentTrackFlow.first()
                val result = CounterReconcilePolicy.reconcile(current.totalLocationPoints, dbCount)
                if (result.isDiverged) {
                    sessionStore.updateLocationCount(resumeToken, result.dbCount, result.dbCount)
                }
            }
        }
        logEvent(
            token,
            if (paused) EventType.TRACKING_PAUSED else EventType.TRACKING_RESUMED,
            if (paused) "Tracking Paused" else "Tracking Resumed",
        )
        statePublisher.update {
            it.copy(
                state = if (paused) TrackingState.PAUSED else TrackingState.LIVE_TRACKING,
                lastEvent = if (paused) EventType.TRACKING_PAUSED else EventType.TRACKING_RESUMED,
            )
        }
        updateNotification(statePublisher.trackingState.value)
    }

    private fun stopAndFinalize() {
        val token = activeToken
        val proc = processor
        fixWatchdogJob?.cancel()
        source?.stop()
        sensorMonitor.stop()
        // P10.5: tear the floating bubble down with the trip (no-op if it was never shown).
        FloatingBubbleService.stop(this)
        statePublisher.update { it.copy(state = TrackingState.COMPLETED, lastEvent = EventType.TRACKING_STOPPED) }
        if (token != null && proc != null) {
            val stats = proc.stats()
            val endTime = System.currentTimeMillis()
            scope.launch(dbDispatcher) {
                // Data-loss guardrail: flush any buffered points before reading DB truth below —
                // dbDispatcher is serial (limitedParallelism(1)), so this completes before the
                // getLocationsByTokenOnce read that follows, in this same coroutine.
                locationBatcher.flush()
                // P-A.2: recompute authoritative cleanedDistance from persisted DB points
                // (consecutive-Haversine excluding isAbnormal/isMock/isPaused). Fall back to
                // in-memory stats when no points are in the DB yet (extremely short trip or error).
                val dbPoints = locationDao.getLocationsByTokenOnce(token)
                val finalCleanedDistanceM =
                    if (dbPoints.isNotEmpty()) {
                        com.mileway.feature.tracking.service.location.DistanceCalculator
                            .computeCleanedDistance(dbPoints)
                    } else {
                        stats.cleanedDistanceM
                    }
                // P-A.4: DB row count is the authoritative finalizer for totalLocationPoints.
                val finalTotalPoints =
                    if (dbPoints.isNotEmpty()) dbPoints.size.toLong() else stats.totalPoints.toLong()
                savedTrackDao.finalizeTrack(
                    routeId = token,
                    endTime = endTime,
                    finalDistance = finalCleanedDistanceM,
                    avgSpeed = stats.avgSpeedMps,
                    maxSpeed = stats.maxSpeedMps,
                )
                // Persist the full advanced distance breakdown into the rich schema fields.
                savedTrackDao.getSavedTrackById(token)?.let { t ->
                    savedTrackDao.updateSavedTrack(
                        t.copy(
                            duration = endTime - startTime,
                            originalDistance = stats.originalDistanceM,
                            cleanedDistance = finalCleanedDistanceM,
                            abnormalDistance = stats.abnormalDistanceM,
                            mockDistance = stats.mockDistanceM,
                            smartDistanceFinal = finalCleanedDistanceM,
                            totalLocationPoints = finalTotalPoints,
                            avgSpeed = stats.avgSpeedMps,
                            maxSpeed = stats.maxSpeedMps,
                            wasMockLocationUsed = stats.mockDistanceM > 0.0,
                            wasEverPaused = t.wasEverPaused,
                        ),
                    )
                }
                logEventSuspend(token, EventType.TRACKING_STOPPED, "Tracking Stopped")
                clearSessionTracking()
                // L.1: refresh the glanceable-surface snapshot now that a trip completed.
                val completed = savedTrackDao.getCompletedTracks().first()
                val freshSnapshot = SurfaceSnapshotProducer.produce(completedTracks = completed, isTracking = false, nowEpochMs = endTime)
                snapshotPublisher.publish(freshSnapshot)
                // P6.1: also persist to the cross-process cache so a cold widget/extension
                // process (no live SnapshotPublisher subscriber) reads the same fresh trip.
                snapshotCache.write(freshSnapshot.toWatchPayload())
            }
        }
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- session + events -------------------------------------------------

    private suspend fun persistSession(
        token: String,
        fix: GpsFix,
        stats: com.mileway.feature.tracking.service.location.TrackStats,
        battery: Double,
    ) {
        val current = currentSession()
        sessionStore.saveSession(
            current.copy(
                token = token,
                isTracking = true,
                isPaused = isPaused,
                startLatitude = proc?.firstLat ?: current.startLatitude,
                startLongitude = proc?.firstLng ?: current.startLongitude,
                endLatitude = fix.lat,
                endLongitude = fix.lng,
                startTime = startTime,
                distance = stats.cleanedDistanceM,
                speed = fix.speedMps.toDouble(),
                avgSpeed = stats.avgSpeedMps,
                maxSpeed = stats.maxSpeedMps,
                totalLocationPoints = stats.totalPoints.toLong(),
                wasEverPaused = current.wasEverPaused || isPaused,
                startedAtTimestamp = if (current.startedAtTimestamp == 0L) startTime else current.startedAtTimestamp,
            ),
        )
    }

    private val proc: LocationProcessor? get() = processor

    private suspend fun currentSession() = sessionStore.currentTrackFlow.first()

    private suspend fun clearSessionTracking() {
        val current = currentSession()
        sessionStore.saveSession(current.copy(isTracking = false, isPaused = false, endTime = System.currentTimeMillis()))
    }

    private fun logEvent(
        token: String,
        type: EventType,
        text: String,
        fix: GpsFix? = null,
    ) {
        scope.launch(dbDispatcher) { logEventSuspend(token, type, text, fix) }
    }

    private suspend fun logEventSuspend(
        token: String,
        type: EventType,
        text: String,
        fix: GpsFix? = null,
    ) {
        smartEventLogger.log(
            HardwareEvent(
                token = token,
                eventType = type,
                event = text,
                time = System.currentTimeMillis(),
                lat = fix?.lat,
                lng = fix?.lng,
                speed = fix?.speedMps,
                audience = EventAudience.USER,
                deviceModel = Build.MODEL ?: "",
                appVersionName = appVersionName(),
            ),
        )
    }

    // ---- device helpers ---------------------------------------------------

    private fun batteryPercent(): Double {
        val bm = getSystemService(BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return pct.toDouble()
    }

    /** Whether the device is currently charging, feeds [DynamicIntervalCalculator] (no battery penalty). */
    private fun isCharging(): Boolean = (getSystemService(BATTERY_SERVICE) as? BatteryManager)?.isCharging ?: false

    /** Whether OS power-saver (battery-saver) mode is on, stretches the GPS cadence. */
    private fun isPowerSaver(): Boolean = (getSystemService(POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false

    private fun appVersionName(): String =
        runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        }.getOrDefault("")

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        wakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mileway:tracking").apply {
                setReferenceCounted(false)
                acquire(60 * 60 * 1000L) // 1h safety cap
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Tracks your current journey" }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * C.2d/P-D.1: build the foreground notification from mapped [TrackingNotificationContent] — title/text
     * per type, dismissible vs ongoing per [TrackingNotificationContent.ongoing], a deep-link tap target
     * (TRIP_COMPLETE → the tracking section) when set, and action buttons from [TrackingNotificationContent.actions].
     */
    private fun buildNotification(content: TrackingNotificationContent): Notification {
        val tapIntent =
            if (content.deepLink != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(content.deepLink)).setPackage(packageName)
            } else {
                Intent(this, TrackMilesActivity::class.java)
            }
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(content.title)
                .setContentText(content.text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(content.ongoing)
                .setContentIntent(contentIntent)
        content.actions.forEachIndexed { i, action ->
            val (label, icon, intentAction) =
                when (action) {
                    TrackingNotificationAction.PAUSE ->
                        Triple("Pause", android.R.drawable.ic_media_pause, ACTION_PAUSE)
                    TrackingNotificationAction.RESUME ->
                        Triple("Resume", android.R.drawable.ic_media_play, ACTION_RESUME)
                    TrackingNotificationAction.STOP ->
                        Triple("Stop", android.R.drawable.ic_delete, ACTION_STOP)
                    TrackingNotificationAction.FIX_GPS ->
                        Triple("Fix GPS", android.R.drawable.ic_menu_mylocation, ACTION_FIX_GPS)
                }
            val pi =
                PendingIntent.getBroadcast(
                    this,
                    100 + i,
                    Intent(intentAction).setPackage(packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, icon),
                    label,
                    pi,
                ).build(),
            )
        }
        return builder.build()
    }

    /**
     * C.2d: publish the phase-aware notification, throttled. Same-type updates within
     * [NOTIFICATION_THROTTLE_MS] are dropped (the live ACTIVE message fires per fix); a type *change*
     * (e.g. ACTIVE → GPS_DISABLED, or → TRIP_COMPLETE) always fires immediately.
     */
    private fun updateNotification(snapshot: TrackingSnapshot) {
        val content = TrackingNotificationMapper.fromSnapshot(snapshot)
        val now = System.currentTimeMillis()
        if (content.type == lastNotificationType && now - lastNotificationAtMs < NOTIFICATION_THROTTLE_MS) return
        lastNotificationType = content.type
        lastNotificationAtMs = now
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(content))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // P-C.1: L1 — user swiped the app from recents while the FGS was running. The service is NOT
    // destroyed immediately on Android 8+; it gets another onStartCommand (START_STICKY) first.
    // We mark the trip record NOW so any subsequent reconciliation knows the session was killed,
    // rather than stopping cleanly, and can deduct the quality score accordingly.
    override fun onTaskRemoved(rootIntent: Intent?) {
        val token = activeToken
        if (token != null) {
            scope.launch(dbDispatcher) {
                savedTrackDao.markAppKilled(token)
                logEventSuspend(token, EventType.APP_KILLED, "App Killed — task removed by user")
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        source?.stop()
        sensorMonitor.stop()
        releaseWakeLock()
        // Data-loss guardrail: onDestroy can arrive without a preceding ACTION_STOP/PAUSE (task
        // swipe → OS teardown, force-stop). Block briefly to flush the batcher before scope.cancel()
        // tears down the dispatcher a coroutine-launched flush would otherwise race.
        if (activeToken != null) {
            runBlocking { locationBatcher.flush() }
        }
        scope.cancel()
        super.onDestroy()
    }
}
