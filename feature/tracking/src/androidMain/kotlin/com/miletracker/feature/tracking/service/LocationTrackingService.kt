package com.miletracker.feature.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.miletracker.core.data.dao.HardwareEventDao
import com.miletracker.core.data.dao.LocationDao
import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.model.db.CurrentTrackData
import com.miletracker.core.data.model.db.EventAudience
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.display.TrackingState
import com.miletracker.core.data.session.CurrentTrackDataStore
import com.miletracker.core.data.settings.DemoSettingsRepository
import com.miletracker.feature.tracking.service.location.DynamicIntervalCalculator
import com.miletracker.feature.tracking.service.location.FusedLocationSource
import com.miletracker.feature.tracking.service.location.GpsFix
import com.miletracker.feature.tracking.service.location.IntervalInputs
import com.miletracker.feature.tracking.service.location.LocationProcessor
import com.miletracker.feature.tracking.service.location.LocationSource
import com.miletracker.feature.tracking.service.location.SimulatedLocationSource
import com.miletracker.feature.tracking.service.location.TrackStats
import com.miletracker.feature.tracking.service.location.TrackingSensorMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val hardwareEventDao: HardwareEventDao by inject()
    private val sessionStore: CurrentTrackDataStore by inject()
    private val demoSettings: DemoSettingsRepository by inject()

    // G6: persisted opt-in for the live Kalman smoother. Collected continuously so the
    // customization toggle takes effect from the next trip start (the smoother is fixed for the
    // lifetime of a running trip; we don't swap algorithms mid-journey).
    @Volatile
    private var enableKalman: Boolean = false

    // C.2b: live telemetry the ViewModel observes via TrackingServiceApi.
    private val statePublisher: TrackingStatePublisher by inject()

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
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001

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
            startForeground(NOTIFICATION_ID, buildNotification("Tracking…"))
            true
        } catch (e: Exception) {
            // e.g. missing FGS location permission on Android 14+, or a background start the
            // OS disallows (ForegroundServiceStartNotAllowedException). Don't crash the app.
            android.util.Log.w("LocationTrackingService", "startForeground failed", e)
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
        scope.launch {
            val session = sessionStore.currentTrackFlow.first()
            val token = intentToken?.takeIf { it.isNotEmpty() } ?: session.token
            val resumable = session.isTracking && token.isNotEmpty() && token == session.token
            withContext(Dispatchers.Main) {
                if (resumable) {
                    startTracking(token, resumeFrom = session)
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
        source = if (SIMULATE_LOCATION) SimulatedLocationSource() else FusedLocationSource(this)
        source?.start { fix -> onFix(token, fix) }
        // A.2: arm the no-fix watchdog for the real-GPS path only.
        if (!SIMULATE_LOCATION) armFirstFixWatchdog(token)
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
        val result = proc.process(fix, isPaused, sensorMonitor.snapshot) ?: return // jitter-suppressed
        val battery = batteryPercent()
        val row = result.location.copy(token = token, batteryPercentage = battery)
        val stats = proc.stats()
        val durationMs = System.currentTimeMillis() - startTime

        scope.launch(dbDispatcher) {
            locationDao.insertLocation(row)
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
                ),
            )
        source?.updateInterval(interval)

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
                lastEvent =
                    when {
                        result.isMock -> EventType.MOCK_LOCATION
                        result.isAbnormal -> EventType.ABNORMAL_LOCATION
                        else -> null
                    },
            )
        }
        updateNotification(notificationText(statePublisher.trackingState.value))
    }

    private fun setPaused(paused: Boolean) {
        // Pause/resume with no live session (service started cold), don't linger foreground.
        val token =
            activeToken ?: run {
                leaveForegroundAndStop()
                return
            }
        isPaused = paused
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
        updateNotification(notificationText(statePublisher.trackingState.value))
    }

    private fun stopAndFinalize() {
        val token = activeToken
        val proc = processor
        fixWatchdogJob?.cancel()
        source?.stop()
        sensorMonitor.stop()
        statePublisher.update { it.copy(state = TrackingState.COMPLETED, lastEvent = EventType.TRACKING_STOPPED) }
        if (token != null && proc != null) {
            val stats = proc.stats()
            val endTime = System.currentTimeMillis()
            scope.launch(dbDispatcher) {
                savedTrackDao.finalizeTrack(
                    routeId = token,
                    endTime = endTime,
                    finalDistance = stats.cleanedDistanceM,
                    avgSpeed = stats.avgSpeedMps,
                    maxSpeed = stats.maxSpeedMps,
                )
                // Persist the full advanced distance breakdown into the rich schema fields.
                savedTrackDao.getSavedTrackById(token)?.let { t ->
                    savedTrackDao.updateSavedTrack(
                        t.copy(
                            duration = endTime - startTime,
                            originalDistance = stats.originalDistanceM,
                            cleanedDistance = stats.cleanedDistanceM,
                            abnormalDistance = stats.abnormalDistanceM,
                            mockDistance = stats.mockDistanceM,
                            smartDistanceFinal = stats.cleanedDistanceM,
                            totalLocationPoints = stats.totalPoints.toLong(),
                            avgSpeed = stats.avgSpeedMps,
                            maxSpeed = stats.maxSpeedMps,
                            wasMockLocationUsed = stats.mockDistanceM > 0.0,
                            wasEverPaused = t.wasEverPaused,
                        ),
                    )
                }
                logEventSuspend(token, EventType.TRACKING_STOPPED, "Tracking Stopped")
                clearSessionTracking()
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
        stats: com.miletracker.feature.tracking.service.location.TrackStats,
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
        hardwareEventDao.insert(
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
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "miletracker:tracking").apply {
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

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Mileway")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    /**
     * Phase-aware foreground notification copy (C.2b), one of seven messages derived from the published
     * [TrackingSnapshot]: acquiring, live, paused, resumed, mock-detected, abnormal-filtered, completed.
     */
    private fun notificationText(s: TrackingSnapshot): String {
        val km = s.distanceMeters / 1000.0
        val kmh = s.speedMps * 3.6
        return when {
            s.state == TrackingState.COMPLETED -> "Journey complete · %.2f km".format(km)
            s.state == TrackingState.PAUSED -> "Paused · %.2f km".format(km)
            s.lastEvent == EventType.MOCK_LOCATION -> "⚠ Mock location detected · %.2f km".format(km)
            s.lastEvent == EventType.ABNORMAL_LOCATION -> "Filtering abnormal GPS · %.2f km".format(km)
            s.lastEvent == EventType.TRACKING_RESUMED -> "Resumed · %.2f km · %.0f km/h".format(km, kmh)
            s.totalPoints == 0 -> "Acquiring GPS…"
            else -> "%.2f km · %.0f km/h".format(km, kmh)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        source?.stop()
        sensorMonitor.stop()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }
}
