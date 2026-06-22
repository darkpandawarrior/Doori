package com.miletracker.feature.tracking.viewmodel

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.model.display.TrackingSystemFlags
import com.miletracker.core.data.model.network.ApprovedVehicle
import com.miletracker.core.data.model.state.TrackMilesPluginConfig
import com.miletracker.core.platform.LocationNameResolver
import com.miletracker.core.platform.OfflineLocationNameResolver
import com.miletracker.core.platform.PlaceName
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.miletracker.feature.tracking.manager.LocationTrackingController
import com.miletracker.feature.tracking.manager.TrackingConfigManager
import com.miletracker.feature.tracking.repository.CurrentTrackRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.repository.VehiclePricingRepository
import com.miletracker.feature.tracking.service.TrackingServiceApi
import com.miletracker.feature.tracking.service.TrackingStatePublisher
import com.miletracker.feature.tracking.ui.sheets.JourneyGuideStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

enum class TrackMilesPhase { IDLE, TRACKING, PAUSED, STOPPED, SUBMITTED }

/** Signal-quality bucket for the gauge accent ring, derived from GPS point density. */
enum class TrackSignal { GOOD, FAIR, POOR }

/** Which face the hero gauge shows. */
enum class HeroGaugeMode { COMPASS, ACTIVITY }

/** Which bottom sheet (if any) the tracking screen is currently presenting. */
enum class TrackSheet { NONE, JOURNEY_GUIDE, VEHICLE_PICKER, VENDOR_PICKER, PAUSE, RESUME, CONSENT }

@Stable
data class TrackMilesUiState(
    val phase: TrackMilesPhase = TrackMilesPhase.IDLE,
    val config: TrackMilesPluginConfig = TrackMilesPluginConfig(),
    val vehicles: List<ApprovedVehicle> = emptyList(),
    val selectedVehicle: ApprovedVehicle? = null,
    val currentRouteId: String? = null,
    val distanceKm: Double = 0.0,
    val durationMs: Long = 0L,
    val reimbursableAmount: Double = 0.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    // ── Live gauge / stats telemetry (from the foreground tracking session) ──────
    val speedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val bearingDegrees: Float = 0f,
    val totalPoints: Long = 0L,
    val unsyncedPoints: Long = 0L,
    val trackingActivity: String = "Stationary",
    val signal: TrackSignal = TrackSignal.GOOD,
    /**
     * Adaptive-engine telemetry (C.3), only the foreground service publishes these via TrackingServiceApi.
     * [gpsIntervalMs] is the current adaptive GPS request cadence (from the DynamicIntervalCalculator).
     */
    val gpsIntervalMs: Long = 0L,
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    // C.3: live quality + health from the snapshot, surfaced as diagnostics chips on the track screen.
    val qualityScore: Int = 100,
    val spikeDistanceM: Double = 0.0,
    val systemFlags: TrackingSystemFlags = TrackingSystemFlags(),
    /**
     * Human-readable current position. Resolved to a place name (e.g. "Koregaon Park, Pune") by the
     * [LocationNameResolver] when available, falling back to formatted coordinates.
     */
    val currentLocationLabel: String = "Waiting for location…",
    /** Raw formatted coordinates of the last fix, shown as the muted secondary line under the name. */
    val currentLocationCoordinates: String = "",
    val gaugeMode: HeroGaugeMode = HeroGaugeMode.COMPASS,
    val pauseReason: String? = null,
    // ── Start-flow / sheet orchestration (single-source-of-truth in the VM) ──────
    val activeSheet: TrackSheet = TrackSheet.NONE,
    val vehicleQuery: String = "",
    val vendorQuery: String = "",
    val startOdometer: Int? = null,
    val draftEnabled: Boolean = false,
    val pauseSelectedReason: String? = null,
    val pauseCustomReason: String = "",
    val resumeNotes: String = "",
    val centers: List<CheckInLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** "This Week: N trips • X.X km", loaded once on init and updated as tracks complete. */
    val weekSummaryText: String = "",
) {
    /** History count surfaced as the small chip on the hero card. */
    val pointsLabel: Int get() = totalPoints.toInt()

    /**
     * G4: VM-owned start-flow step for the [JourneyGuideSheet] stepper — the single source of truth.
     * The screen no longer derives this inline. Runtime permissions are gated *before* the guide sheet
     * opens (see `requestStartTracking`), so the in-sheet stepper starts at [JourneyGuideStep.VEHICLE]
     * and advances to [JourneyGuideStep.TRACKING] once a vehicle is selected.
     */
    val journeyStep: JourneyGuideStep
        get() = if (selectedVehicle == null) JourneyGuideStep.VEHICLE else JourneyGuideStep.TRACKING
}

class TrackMilesViewModel(
    private val configManager: TrackingConfigManager,
    private val vehicleRepo: VehiclePricingRepository,
    private val trackRepo: SavedTrackRepository,
    private val trackingController: LocationTrackingController,
    private val currentTrackRepo: CurrentTrackRepository,
    private val locationRepo: LocationRepository,
    private val geoCheckInLocations: List<CheckInLocation> = emptyList(),
    // C.3: live feed from the foreground service. Defaulted so JVM tests can omit it; Koin injects the
    // singleton TrackingStatePublisher in production (it ignores the default).
    private val trackingServiceApi: TrackingServiceApi = TrackingStatePublisher(),
    // Reverse geocoding → place names for the live location label. Defaulted to the offline resolver
    // so JVM tests (and any graph that omits platformModule) still resolve real-looking names without
    // a network; Koin injects the bound LocationNameResolver in production.
    private val locationNameResolver: LocationNameResolver = OfflineLocationNameResolver(),
) : BaseViewModel<TrackMilesUiState, TrackMilesEffect, TrackMilesAction>(TrackMilesUiState()) {
    /** Backwards-compatible alias; screens read [state]. */
    val uiState: StateFlow<TrackMilesUiState> = state

    private var liveObserveJob: Job? = null
    private var sessionObserveJob: Job? = null
    private var bearingObserveJob: Job? = null
    private var trackingStateObserveJob: Job? = null

    /** Last reverse-geocode result, keyed by ~11 m coordinate cell, to avoid redundant lookups. */
    private var lastResolvedCell: Pair<String, PlaceName>? = null

    init {
        loadConfig()
        loadVehicles()
        observeSession()
        observeTrackingState()
        restoreActiveTrack()
        loadWeekSummary()
    }

    /** Routes screen intents to the handlers below (handlers stay public for unit tests). */
    override fun onAction(action: TrackMilesAction) {
        when (action) {
            TrackMilesAction.OpenJourneyGuide -> openJourneyGuide()
            TrackMilesAction.DismissSheet -> dismissSheet()
            TrackMilesAction.OpenVehiclePicker -> openVehiclePicker()
            is TrackMilesAction.SetVehicleQuery -> setVehicleQuery(action.query)
            is TrackMilesAction.PickVehicle -> pickVehicle(action.key)
            is TrackMilesAction.SelectVehicle -> selectVehicle(action.vehicle)
            TrackMilesAction.CaptureStartOdometer -> captureStartOdometer()
            is TrackMilesAction.ToggleDraft -> toggleDraft(action.enabled)
            TrackMilesAction.OpenVendorPicker -> openVendorPicker()
            is TrackMilesAction.SetVendorQuery -> setVendorQuery(action.query)
            is TrackMilesAction.PickVendor -> pickVendor(action.id)
            TrackMilesAction.OpenPauseSheet -> openPauseSheet()
            is TrackMilesAction.SetPauseReason -> setPauseReason(action.reason)
            is TrackMilesAction.SetPauseCustomReason -> setPauseCustomReason(action.text)
            is TrackMilesAction.ConfirmPause -> confirmPause(action.reason)
            TrackMilesAction.OpenResumeSheet -> openResumeSheet()
            is TrackMilesAction.SetResumeNotes -> setResumeNotes(action.notes)
            TrackMilesAction.ConfirmResume -> confirmResume()
            TrackMilesAction.RequestStartTracking -> requestStartTracking()
            TrackMilesAction.AcceptConsentAndStart -> acceptConsentAndStart()
            TrackMilesAction.StopTracking -> stopTracking()
            TrackMilesAction.DiscardTracking -> discardTracking()
            TrackMilesAction.ToggleGaugeMode -> toggleGaugeMode()
        }
    }

    /**
     * Stream the live foreground session (speed, point counts, activity, pause reason)
     * straight into the gauge/stats telemetry. Independent of the per-route distance feed.
     */
    private fun observeSession() {
        sessionObserveJob?.cancel()
        sessionObserveJob =
            currentTrackRepo.currentTrackFlow
                .onEach { s ->
                    // Signal quality from point density: dense fixes = good, sparse = poor.
                    val signal =
                        when {
                            s.totalLocationPoints >= 8 -> TrackSignal.GOOD
                            s.totalLocationPoints >= 3 -> TrackSignal.FAIR
                            else -> TrackSignal.POOR
                        }
                    setState {
                        copy(
                            speedKmh = s.speed * 3.6,
                            avgSpeedKmh = s.avgSpeed * 3.6,
                            maxSpeedKmh = s.maxSpeed * 3.6,
                            totalPoints = s.totalLocationPoints,
                            unsyncedPoints = s.unsyncedLocationPoints,
                            trackingActivity = s.trackingActivity.ifBlank { "Stationary" },
                            pauseReason = s.pauseReason,
                            signal = signal,
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    /**
     * Subscribe to the foreground service's live [TrackingServiceApi.trackingState] (C.3). This carries
     * the transient adaptive-engine telemetry the persisted session does not, the dynamic GPS cadence
     * and charging/battery state, which we surface for the diagnostics chips. The per-fix speed/points
     * gauge stays on the [observeSession] DataStore feed (it also carries sensor-derived activity +
     * unsynced counts), so the two channels are complementary rather than double-writing the same fields.
     */
    private fun observeTrackingState() {
        trackingStateObserveJob?.cancel()
        trackingStateObserveJob =
            trackingServiceApi.trackingState
                .onEach { snap ->
                    setState {
                        copy(
                            gpsIntervalMs = snap.currentIntervalMs,
                            batteryPct = snap.batteryPct,
                            isCharging = snap.isCharging,
                            qualityScore = snap.qualityScore,
                            spikeDistanceM = snap.spikeDistanceM,
                            systemFlags = snap.systemFlags,
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    /**
     * Synthesize the gauge bearing from the live route. Emulators report no magnetometer,
     * so we use the last fix's pipeline-computed bearing, falling back to the heading
     * between the last two points.
     */
    private fun observeBearing(routeId: String) {
        bearingObserveJob?.cancel()
        bearingObserveJob =
            locationRepo.locationsForToken(routeId)
                .onEach { points ->
                    if (points.isEmpty()) return@onEach
                    val last = points.last()
                    val bearing =
                        when {
                            last.bearing != 0f -> last.bearing
                            points.size >= 2 -> headingBetween(points[points.size - 2], last)
                            else -> 0f
                        }
                    // Reverse-geocode the fix to a human-readable place name (offline-safe; never
                    // throws). Keep the formatted coordinates as the muted secondary line.
                    val place = resolvePlaceName(last.lat, last.lng)
                    setState {
                        copy(
                            bearingDegrees = bearing,
                            currentLocationLabel = place.displayLabel,
                            currentLocationCoordinates = place.coordinates,
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    /**
     * Resolve a fix to a [PlaceName], suppressing repeat lookups for coordinates that round to the
     * same ~11 m cell so a dense GPS feed doesn't re-geocode every fix. Falls back to coordinates on
     * any failure (the resolver itself never throws).
     */
    private suspend fun resolvePlaceName(
        lat: Double,
        lng: Double,
    ): PlaceName {
        val cellKey = "${(lat * 10_000).toLong()}:${(lng * 10_000).toLong()}"
        lastResolvedCell?.let { (key, cached) -> if (key == cellKey) return cached }
        val resolved = locationNameResolver.resolve(lat, lng)
        lastResolvedCell = cellKey to resolved
        return resolved
    }

    private fun loadConfig() {
        setState { copy(config = configManager.getTrackMilesConfig(), centers = geoCheckInLocations) }
    }

    // ── Start-flow & sheet orchestration (MVI intents) ──────────────────────────

    fun openJourneyGuide() = setState { copy(activeSheet = TrackSheet.JOURNEY_GUIDE) }

    fun dismissSheet() = setState { copy(activeSheet = TrackSheet.NONE) }

    fun openVehiclePicker() = setState { copy(activeSheet = TrackSheet.VEHICLE_PICKER) }

    fun setVehicleQuery(q: String) = setState { copy(vehicleQuery = q) }

    /** Pick a vehicle by key from the [vehicles] list and return to the journey guide. */
    fun pickVehicle(key: String) {
        val vehicle = currentState.vehicles.firstOrNull { it.vehicleKey == key } ?: return
        setState { copy(selectedVehicle = vehicle, activeSheet = TrackSheet.JOURNEY_GUIDE, vehicleQuery = "") }
    }

    /** Simulate an odometer capture (the demo has no real OCR in this flow). */
    fun captureStartOdometer() {
        // Deterministic mock reading so the checklist completes without a camera round-trip.
        val reading = 45_000 + (currentState.vehicles.size * 57)
        setState { copy(startOdometer = reading) }
    }

    fun toggleDraft(enabled: Boolean) = setState { copy(draftEnabled = enabled) }

    /** "Start Tracking" pressed in the guide: show consent if configured, else start now. */
    fun requestStartTracking() {
        val disclaimer = configManager.getJourneyDisclaimer()
        if (!disclaimer.isNullOrBlank()) {
            setState { copy(activeSheet = TrackSheet.CONSENT) }
        } else {
            beginTracking()
        }
    }

    fun acceptConsentAndStart() = beginTracking()

    private fun beginTracking() {
        setState { copy(activeSheet = TrackSheet.NONE) }
        startTracking()
    }

    // Pause / resume sheets
    fun openPauseSheet() = setState { copy(activeSheet = TrackSheet.PAUSE) }

    fun setPauseReason(reason: String?) = setState { copy(pauseSelectedReason = reason) }

    fun setPauseCustomReason(text: String) = setState { copy(pauseCustomReason = text) }

    fun confirmPause(reason: String) {
        setState { copy(activeSheet = TrackSheet.NONE) }
        pauseTracking(reason)
    }

    fun openResumeSheet() = setState { copy(activeSheet = TrackSheet.RESUME) }

    fun setResumeNotes(notes: String) = setState { copy(resumeNotes = notes) }

    fun confirmResume() {
        setState { copy(activeSheet = TrackSheet.NONE, resumeNotes = "") }
        resumeTracking()
    }

    // Vendor / center picker
    fun openVendorPicker() = setState { copy(activeSheet = TrackSheet.VENDOR_PICKER) }

    fun setVendorQuery(q: String) = setState { copy(vendorQuery = q) }

    fun pickVendor(id: String) {
        // Selecting a center is acknowledged; the demo records it implicitly via check-in.
        setState { copy(activeSheet = TrackSheet.NONE, vendorQuery = "") }
    }

    private fun loadWeekSummary() {
        trackRepo.completedTracksFlow()
            .onEach { tracks ->
                val weekStartMs = System.currentTimeMillis() - 7L * 24 * 3_600_000
                val thisWeek = tracks.filter { it.endTime >= weekStartMs }
                val totalKm = thisWeek.sumOf { it.distanceKm }
                val text =
                    if (thisWeek.isEmpty()) {
                        "No journeys this week"
                    } else {
                        "This Week: ${thisWeek.size} trip${if (thisWeek.size != 1) "s" else ""} • ${"%.1f".format(totalKm)} km"
                    }
                setState { copy(weekSummaryText = text) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            runCatching { vehicleRepo.getVehicles(trackMiles = true) }
                .onSuccess { vehicles ->
                    setState { copy(vehicles = vehicles, selectedVehicle = vehicles.firstOrNull()) }
                }
                .onFailure { Log.w("TrackMilesVM", "Failed to load vehicles", it) }
        }
    }

    private fun restoreActiveTrack() {
        viewModelScope.launch {
            val active = trackRepo.getActiveTrack() ?: return@launch
            setState {
                copy(
                    phase = TrackMilesPhase.TRACKING,
                    currentRouteId = active.routeId,
                    distanceKm = active.distance / 1000.0,
                    startTime = active.startTime,
                )
            }
            observeLive(active.routeId)
            // A.2: also resume the bearing/location-label feed so a restored session immediately
            // shows live coordinates instead of staying stuck on "Waiting for location…".
            observeBearing(active.routeId)
        }
    }

    fun selectVehicle(vehicle: ApprovedVehicle) {
        setState { copy(selectedVehicle = vehicle) }
    }

    fun startTracking() {
        val vehicle = currentState.selectedVehicle ?: return
        val routeId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val track =
                SavedTrack(
                    routeId = routeId,
                    name = "Journey ${java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))}",
                    startLatitude = 0.0, startLongitude = 0.0,
                    endLatitude = 0.0, endLongitude = 0.0,
                    pausedLatitude = 0.0, pausedLongitude = 0.0,
                    startTime = now, endTime = -1L,
                    distance = 0.0, duration = 0L,
                    selectedVehicleType = vehicle.vehicleKey ?: "",
                    vehiclePricing = vehicle.vehiclePricing ?: 0.0,
                    createdAt = now, startedAtTimestamp = now, startedByEmployeeCode = "EMP001",
                )
            trackRepo.insert(track)
            setState {
                copy(phase = TrackMilesPhase.TRACKING, currentRouteId = routeId, distanceKm = 0.0, startTime = now)
            }
            // Kick off the advanced foreground tracking service and observe its live writes.
            trackingController.start(routeId)
            observeLive(routeId)
            observeBearing(routeId)
        }
    }

    /** Toggle the hero gauge between the compass face and the activity timeline. */
    fun toggleGaugeMode() {
        setState {
            copy(
                gaugeMode =
                    if (gaugeMode == HeroGaugeMode.COMPASS) HeroGaugeMode.ACTIVITY else HeroGaugeMode.COMPASS,
            )
        }
    }

    /** Stream the service's live writes to `saved_tracks` into the UI in real time. */
    private fun observeLive(routeId: String) {
        liveObserveJob?.cancel()
        liveObserveJob =
            trackRepo.observeByRouteId(routeId)
                .onEach { track ->
                    if (track == null) return@onEach
                    val pricing = currentState.selectedVehicle?.vehiclePricing ?: track.vehiclePricing
                    val km = track.distance / 1000.0
                    setState { copy(distanceKm = km, durationMs = track.duration, reimbursableAmount = km * pricing) }
                }
                .launchIn(viewModelScope)
    }

    fun pauseTracking(reason: String? = null) {
        currentState.currentRouteId?.let { trackingController.pause(it) }
        setState { copy(phase = TrackMilesPhase.PAUSED, pauseReason = reason) }
    }

    fun resumeTracking() {
        currentState.currentRouteId?.let { trackingController.resume(it) }
        setState { copy(phase = TrackMilesPhase.TRACKING) }
    }

    fun stopTracking() {
        currentState.currentRouteId?.let { trackingController.stop(it) }
        liveObserveJob?.cancel()
        setState { copy(phase = TrackMilesPhase.STOPPED, endTime = System.currentTimeMillis()) }
    }

    fun updateDistance(km: Double) {
        val pricing = currentState.selectedVehicle?.vehiclePricing ?: 0.0
        setState { copy(distanceKm = km, reimbursableAmount = km * pricing) }
    }

    fun discardTracking() {
        currentState.currentRouteId?.let { trackingController.stop(it) }
        liveObserveJob?.cancel()
        bearingObserveJob?.cancel()
        setState { TrackMilesUiState(config = config, vehicles = vehicles, selectedVehicle = selectedVehicle) }
    }

    override fun onCleared() {
        liveObserveJob?.cancel()
        sessionObserveJob?.cancel()
        bearingObserveJob?.cancel()
        trackingStateObserveJob?.cancel()
        super.onCleared()
    }

    private companion object {
        /** Great-circle initial bearing (degrees, 0–360) between two fixes. */
        fun headingBetween(
            a: com.miletracker.core.data.model.db.LocationData,
            b: com.miletracker.core.data.model.db.LocationData,
        ): Float {
            val lat1 = a.lat * kotlin.math.PI / 180.0
            val lat2 = b.lat * kotlin.math.PI / 180.0
            val dLon = (b.lng - a.lng) * kotlin.math.PI / 180.0
            val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
            val x =
                kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                    kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
            val deg = kotlin.math.atan2(y, x) * 180.0 / kotlin.math.PI
            return ((deg + 360.0) % 360.0).toFloat()
        }
    }
}
