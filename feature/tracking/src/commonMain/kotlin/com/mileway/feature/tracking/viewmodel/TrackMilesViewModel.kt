package com.mileway.feature.tracking.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.location.DESTINATION_GUEST_KEY
import com.mileway.core.data.location.DestinationModeRepository
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.DelegationSessionSource
import com.mileway.core.data.session.NoDelegationSessionSource
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.session.TripOwnershipBinding
import com.mileway.core.data.session.doesSessionBelongTo
import com.mileway.core.data.session.effectiveSignedInIdentity
import com.mileway.core.data.session.from
import com.mileway.core.data.vehicle.GarageRepository
import com.mileway.core.data.vehicle.VehicleCatalog
import com.mileway.core.data.vehicle.VehicleRateRepository
import com.mileway.core.data.vehicle.reimbursableAmount
import com.mileway.core.network.ReadState
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.service.TrackingStatePublisher
import com.mileway.feature.tracking.ui.sheets.JourneyGuideStep
import com.siddharth.kmp.appshell.LocationNameResolver
import com.siddharth.kmp.appshell.PlaceName
import com.siddharth.kmp.mvi.BaseViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class TrackMilesPhase { IDLE, TRACKING, PAUSED, STOPPED, SUBMITTED }

/** Signal-quality bucket for the gauge accent ring, derived from GPS point density. */
enum class TrackSignal { GOOD, FAIR, POOR }

/** Which face the hero gauge shows. */
enum class HeroGaugeMode { COMPASS, ACTIVITY }

/** Which bottom sheet (if any) the tracking screen is currently presenting. */
enum class TrackSheet {
    NONE,
    JOURNEY_GUIDE,
    VEHICLE_PICKER,
    VENDOR_PICKER,
    PAUSE,
    RESUME,
    CONSENT,
    SESSION_RESTORE,
    STRANGER_SESSION,
}

/** P-C.5: data surfaced in the session-restore bottom sheet. */
data class RecoverySheetConfig(
    val token: String,
    val distanceKm: Double,
    val durationMs: Long,
    val interruptReason: String,
)

/**
 * P3.5: cold-start reconciliation — a persisted trip whose `started_by_*` ownership pointer
 * doesn't match the currently-active persona. [ownerLabel] is the other persona's display name
 * (or employee code if the persona record can't be resolved), shown in the "Resume session for
 * {other persona}?" dialog instead of silently restoring a stranger's trip.
 */
data class StrangerSessionConfig(
    val routeId: String,
    val ownerLabel: String,
)

/**
 * C4: the full tracking journey's state machine, modeled explicitly rather than inferred ad hoc
 * from `phase`/`selectedVehicle`/`activeSheet` at each call site. Broader than [JourneyGuideStep]
 * (which only drives the journey-guide sheet's 3-checkpoint visual stepper) — this covers every
 * checkpoint from permission grant through post-trip submission. See
 * [TrackMilesUiState.journeyProgress] for the derivation.
 */
enum class JourneyStep {
    /** Runtime location/notification permissions are not yet satisfied. */
    PERMISSIONS,

    /** No vehicle is selected yet. */
    VEHICLE,

    /** A vehicle is selected but the (mandatory) start odometer hasn't been captured. */
    START_ODOMETER,

    /** All prerequisites are met; tracking can begin. */
    READY_TO_START,

    /** A trip is live (tracking or paused). */
    TRACKING,

    /** The trip has stopped and is ready to be reviewed/submitted. */
    READY_TO_SUBMIT,
}

/**
 * C4: the single hero-CTA action for a given [TrackMilesUiState], so the screen reads one value
 * instead of re-deriving it from `phase` + `activeSheet` + `selectedVehicle` inline. See
 * [TrackMilesUiState.primaryAction].
 */
sealed interface TrackMilesPrimaryAction {
    data object ResolvePermissions : TrackMilesPrimaryAction

    data object SelectVehicle : TrackMilesPrimaryAction

    data object CaptureStartOdometer : TrackMilesPrimaryAction

    data object CaptureEndOdometer : TrackMilesPrimaryAction

    data object StartTracking : TrackMilesPrimaryAction

    data object PauseTracking : TrackMilesPrimaryAction

    data object ResumeTracking : TrackMilesPrimaryAction

    data object StopTracking : TrackMilesPrimaryAction

    data object SubmitTrack : TrackMilesPrimaryAction
}

/**
 * C4: richer odometer-capture state, additive alongside the bare `Int?` reading the journey-guide
 * sheet has always used (see [TrackMilesUiState.odometer]). Carries both start and end readings so
 * [JourneyStep.READY_TO_SUBMIT] and [TrackMilesPrimaryAction.CaptureEndOdometer] have real state to
 * key off, plus the reconciliation fields ([computedDistance], [validationError]) that a future
 * end-odometer capture screen will surface.
 */
data class OdometerState(
    val startReading: Int? = null,
    val endReading: Int? = null,
    val startPhotoUrl: String? = null,
    val endPhotoUrl: String? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
) {
    /** Distance implied by the two odometer readings, or `null` until both are captured. */
    val computedDistance: Int?
        get() {
            val start = startReading ?: return null
            val end = endReading ?: return null
            return (end - start).takeIf { it >= 0 }
        }

    /** Non-null when the two readings can't be reconciled (end before start). */
    val validationError: String?
        get() {
            val start = startReading ?: return null
            val end = endReading ?: return null
            return if (end < start) "End odometer reading is before the start reading" else null
        }
}

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
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val gaugeMode: HeroGaugeMode = HeroGaugeMode.COMPASS,
    val pauseReason: String? = null,
    // ── Start-flow / sheet orchestration (single-source-of-truth in the VM) ──────
    val activeSheet: TrackSheet = TrackSheet.NONE,
    val activeRecovery: RecoverySheetConfig? = null,
    val activeStrangerSession: StrangerSessionConfig? = null,
    val vehicleQuery: String = "",
    val vendorQuery: String = "",
    // C4: replaces the previous bare `startOdometer: Int?` with the richer OdometerState (start +
    // end readings, photo/label metadata, computed distance/validation) — see OdometerState.
    val odometer: OdometerState = OdometerState(),
    // C4: runtime-permission gating lives at the screen level today (PermissionOnboardingFlow in
    // TrackMilesScreen runs BEFORE any action reaches this ViewModel — see requestStartTracking()
    // there), so the VM has no real signal to flip this. Defaulted true so journeyProgress/
    // primaryAction behave exactly as before for every existing caller; wiring a real signal in is
    // a later task.
    val permissionsSatisfied: Boolean = true,
    val draftEnabled: Boolean = false,
    val pauseSelectedReason: String? = null,
    val pauseCustomReason: String = "",
    val resumeNotes: String = "",
    val centers: List<CheckInLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** "This Week: N trips • X.X km", loaded once on init and updated as tracks complete. */
    val weekSummaryText: String = "",
    /** P-D.3: formatted countdown string shown in the hero-card chip while auto-discard is pending. */
    val autoDiscardCountdown: String? = null,
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

    /**
     * C4: the full-flow [JourneyStep], superseding [journeyStep]'s 3-checkpoint model with the
     * complete permissions→vehicle→odometer→ready→tracking→submit machine. Named distinctly from
     * [journeyStep] (rather than replacing it) because an existing test
     * (`app/src/test/.../TrackMilesViewModelTest.journeyStep is VM-owned...`) asserts the exact
     * `journeyStep`/[JourneyGuideStep] pairing — that mapping still holds unchanged.
     */
    val journeyProgress: JourneyStep
        get() =
            when {
                phase == TrackMilesPhase.TRACKING || phase == TrackMilesPhase.PAUSED -> JourneyStep.TRACKING
                phase == TrackMilesPhase.STOPPED || phase == TrackMilesPhase.SUBMITTED -> JourneyStep.READY_TO_SUBMIT
                !permissionsSatisfied -> JourneyStep.PERMISSIONS
                selectedVehicle == null -> JourneyStep.VEHICLE
                config.isOdometerMandatory && odometer.startReading == null -> JourneyStep.START_ODOMETER
                else -> JourneyStep.READY_TO_START
            }

    /**
     * C4: the single hero-CTA the screen should show for the current state, so it reads one value
     * instead of inferring from `phase` + `activeSheet` + `selectedVehicle` inline (see the current
     * `onHero` branch in TrackMilesScreen, which duplicates this same `isActive` logic ad hoc).
     * Mirrors [journeyProgress]'s prerequisite ladder; [TrackMilesPrimaryAction.PauseTracking] /
     * [TrackMilesPrimaryAction.ResumeTracking] are modeled for completeness (the pause/resume
     * button is a separate, already-explicit toggle in the screen) even though this particular
     * derivation picks [TrackMilesPrimaryAction.StopTracking] while active, matching the FAB's
     * actual current behavior.
     */
    val primaryAction: TrackMilesPrimaryAction
        get() =
            when {
                phase == TrackMilesPhase.TRACKING || phase == TrackMilesPhase.PAUSED -> TrackMilesPrimaryAction.StopTracking
                phase == TrackMilesPhase.STOPPED && odometer.endReading == null -> TrackMilesPrimaryAction.CaptureEndOdometer
                phase == TrackMilesPhase.STOPPED || phase == TrackMilesPhase.SUBMITTED -> TrackMilesPrimaryAction.SubmitTrack
                !permissionsSatisfied -> TrackMilesPrimaryAction.ResolvePermissions
                selectedVehicle == null -> TrackMilesPrimaryAction.SelectVehicle
                config.isOdometerMandatory && odometer.startReading == null -> TrackMilesPrimaryAction.CaptureStartOdometer
                else -> TrackMilesPrimaryAction.StartTracking
            }
}

/** P3.3: default [SessionSource] for JVM tests (and Koin graphs) that omit a real one — always signed-out. */
object NoSessionSource : SessionSource {
    override val sessionState = flowOf(SessionState())
}

/** P3.5: default [ActiveAccountSource] for JVM tests (and Koin graphs) that omit a real one — no active persona pointer. */
object NoActiveAccountSource : ActiveAccountSource {
    override val activeAccountId = flowOf<String?>(null)

    override suspend fun setActiveAccountId(accountId: String) = Unit
}

class TrackMilesViewModel(
    private val configManager: TrackingConfigManager,
    private val vehicleRepo: VehiclePricingRepository,
    private val trackRepo: SavedTrackRepository,
    private val trackingController: TrackingController,
    private val currentTrackRepo: CurrentTrackRepository,
    private val locationRepo: LocationRepository,
    // P4.x: persists the pause reason as a TRACKING_PAUSED hardware event (existing `metadata`
    // field — no schema change). Defaulted so JVM tests can omit it.
    private val hardwareEventRepo: HardwareEventRepository? = null,
    private val geoCheckInLocations: List<CheckInLocation> = emptyList(),
    // C.3: live feed from the foreground service. Defaulted so JVM tests can omit it; Koin injects the
    // singleton TrackingStatePublisher in production (it ignores the default).
    private val trackingServiceApi: TrackingServiceApi = TrackingStatePublisher(),
    // Reverse geocoding → place names for the live location label. Defaulted to the offline resolver
    // so JVM tests (and any graph that omits platformModule) still resolve real-looking names without
    // a network; Koin injects the bound LocationNameResolver in production.
    private val locationNameResolver: LocationNameResolver = OfflineLocationNameResolver(),
    // P-C.5: bridge from app-startup reconciliation to this ViewModel. Null in JVM tests.
    private val reconciliationHolder: ReconciliationResultHolder? = null,
    // P3.3: signed-in identity source for stamping a new trip's started_by_* ownership pointer.
    // Defaulted to an always-signed-out source so JVM tests can omit it; Koin injects the real
    // SessionRepository (androidMain/iosMain) in production.
    private val sessionSource: SessionSource = NoSessionSource,
    // P3.5: which persona is currently active, for cold-start ownership reconciliation. Defaulted
    // so JVM tests can omit it; Koin injects the real ActiveAccountStore in production.
    private val activeAccountSource: ActiveAccountSource = NoActiveAccountSource,
    // P3.5: resolves the other persona's display name for the "Resume session for {other
    // persona}?" dialog. Null in JVM tests/graphs that omit it — falls back to the raw employee
    // code in that case.
    private val mockAccountDao: MockAccountDao? = null,
    // PLAN_V24 P7.3: the session-delegation overlay. While acting on behalf of a reportee, a new
    // trip is stamped with (and reconciled against) the ACTING identity, not the base persona's —
    // see effectiveSignedInIdentity. Defaulted to the never-acting source so JVM tests can omit it;
    // Koin injects the real DelegationSessionController (androidMain/iosMain) in production.
    private val delegationSource: DelegationSessionSource = NoDelegationSessionSource,
    // PLAN_V24 P11.1: per-km policy-rate source. When present, it overlays each vehicle's rate with
    // the persona-gated catalog rate (off ⇒ no rate). Null in JVM tests/graphs that omit it — the
    // vehicles keep whatever pricing the (stub) vehicle source already carried, so those tests and
    // the screenshot harness stay unchanged.
    private val vehicleRateRepo: VehicleRateRepository? = null,
    // PLAN_V24 P11.2: the garage's active vehicle drives the default selected vehicle here (so the
    // vehicle chosen in the garage is pre-selected for a new trip). Null in JVM tests/graphs that
    // omit it — the default stays the first vehicle in the list.
    private val garageRepo: GarageRepository? = null,
    // PLAN_V24 P11.3: head-home destination store. When a destination is active at trip start, the
    // starting trip is auto-classified toward it (SavedTrack.destinationTag). Null in graphs that
    // omit core:data — trips simply carry no destination tag then.
    private val destinationModeRepo: DestinationModeRepository? = null,
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
        observeReconciliationResult()
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
            TrackMilesAction.CaptureEndOdometer -> captureEndOdometer()
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
            // P-C.5: restore-sheet outcomes.
            TrackMilesAction.RecoveryResume -> handleRecoveryResume()
            TrackMilesAction.RecoverySaveFinish -> handleRecoverySaveFinish()
            TrackMilesAction.RecoveryDiscard -> handleRecoveryDiscard()
            // P3.5: cold-start ownership-mismatch dialog outcomes.
            TrackMilesAction.StrangerSessionResume -> handleStrangerSessionResume()
            TrackMilesAction.StrangerSessionDismiss -> handleStrangerSessionDismiss()
            // Wave-4 §2.1: multi-session restore sheet outcome.
            is TrackMilesAction.MultiSessionResume -> handleMultiSessionResume(action.routeId)
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
                            currentLat = last.lat,
                            currentLng = last.lng,
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
        setState {
            copy(odometer = odometer.copy(startReading = reading, startTimeMs = Clock.System.now().toEpochMilliseconds()))
        }
    }

    /**
     * C4: end-of-trip odometer capture, mirroring [captureStartOdometer]'s deterministic mock
     * reading. Ties the reading to the accumulated GPS distance so [OdometerState.computedDistance]
     * comes out sane; a real camera/OCR capture UI is a later task.
     */
    fun captureEndOdometer() {
        val start = currentState.odometer.startReading ?: 45_000
        val reading = start + currentState.distanceKm.roundToLong().toInt()
        setState {
            copy(odometer = odometer.copy(endReading = reading, endTimeMs = Clock.System.now().toEpochMilliseconds()))
        }
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
                val weekStartMs = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 3_600_000
                val thisWeek = tracks.filter { it.endTime >= weekStartMs }
                val totalKm = thisWeek.sumOf { it.distanceKm }
                val text =
                    if (thisWeek.isEmpty()) {
                        "No journeys this week"
                    } else {
                        "This Week: ${thisWeek.size} trip${if (thisWeek.size != 1) "s" else ""} • ${totalKm.fmt1()} km"
                    }
                setState { copy(weekSummaryText = text) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * PLAN_V33 A6: cache-then-refresh — [VehiclePricingRepository.vehiclesState] emits the
     * last-cached vehicle list immediately (so this screen renders offline), then a fresh list
     * once a network refresh completes.
     */
    private fun loadVehicles() {
        vehicleRepo.vehiclesState(trackMiles = true)
            .onEach { readState ->
                when (readState) {
                    is ReadState.Content -> {
                        // P11.1: overlay per-km policy rates gated by the active persona. When rates
                        // are off for this persona, pricing is null (no ₹/km chip, no reimbursable amount).
                        val loaded = readState.data
                        val vehicles =
                            vehicleRateRepo?.let { repo ->
                                val enabled = repo.ratesEnabled()
                                loaded.map { v ->
                                    v.copy(vehiclePricing = if (enabled) VehicleCatalog.rateFor(v.vehicleKey.orEmpty()) else null)
                                }
                            } ?: loaded
                        // P11.2: default to the garage's active vehicle type when one is set.
                        val activeKey = garageRepo?.getActive()?.vehicleTypeKey
                        val default = vehicles.firstOrNull { it.vehicleKey == activeKey } ?: vehicles.firstOrNull()
                        setState { copy(vehicles = vehicles, selectedVehicle = default) }
                    }
                    ReadState.Loading -> Unit
                    is ReadState.Error -> Napier.w("Failed to load vehicles: ${readState.message}", tag = "TrackMilesVM")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun restoreActiveTrack() {
        viewModelScope.launch {
            val active = trackRepo.getActiveTrack() ?: return@launch

            // P3.5: cold-start reconciliation — verify the restored trip's started_by_* ownership
            // pointer belongs to the currently-active persona before silently displaying it. This
            // is the local, offline half of the reference app's syncWithServerOngoingSession
            // reconciliation; the multi-device server-reconciliation half is out of scope here
            // (see PLAN_V22 §6 — irreducibly backend-shaped).
            if (isStrangerSession(active)) {
                val ownerLabel = resolveOwnerLabel(active.startedByEmployeeCode)
                setState {
                    copy(
                        activeSheet = TrackSheet.STRANGER_SESSION,
                        activeStrangerSession = StrangerSessionConfig(routeId = active.routeId, ownerLabel = ownerLabel),
                    )
                }
                return@launch
            }

            restoreTrackIntoState(active.routeId, active.distance, active.startTime)
        }
    }

    /** True when [track]'s ownership pointer doesn't match the currently-active persona's identity. */
    private suspend fun isStrangerSession(track: SavedTrack): Boolean {
        val activeAccountId = activeAccountSource.activeAccountId.first() ?: return false
        val session = sessionSource.sessionState.first()
        // P7.3: while acting on behalf of a reportee, the current identity IS the delegate's, so a
        // trip the base persona started reads as a stranger session (and vice-versa).
        val currentIdentity =
            effectiveSignedInIdentity(activeAccountId, session, delegationSource.delegationState.first())
        val binding = TripOwnershipBinding.from(track)
        return !doesSessionBelongTo(binding, currentIdentity)
    }

    /** Resolves the other persona's display name for the dialog, falling back to the raw employee code. */
    private suspend fun resolveOwnerLabel(employeeCode: String): String {
        if (employeeCode.isBlank()) return "another persona"
        val owner = mockAccountDao?.observeAll()?.first()?.firstOrNull { it.employeeCode == employeeCode }
        return owner?.displayName ?: employeeCode
    }

    private fun restoreTrackIntoState(
        routeId: String,
        distanceMeters: Double,
        startTime: Long,
    ) {
        setState {
            copy(
                phase = TrackMilesPhase.TRACKING,
                currentRouteId = routeId,
                distanceKm = distanceMeters / 1000.0,
                startTime = startTime,
            )
        }
        observeLive(routeId)
        // A.2: also resume the bearing/location-label feed so a restored session immediately
        // shows live coordinates instead of staying stuck on "Waiting for location…".
        observeBearing(routeId)
    }

    /** "Resume session for {other persona}?" confirmed — restores the trip as today. */
    fun handleStrangerSessionResume() {
        val config = currentState.activeStrangerSession ?: return
        viewModelScope.launch {
            val track = trackRepo.getByRouteId(config.routeId)
            setState { copy(activeSheet = TrackSheet.NONE, activeStrangerSession = null) }
            track?.let { restoreTrackIntoState(it.routeId, it.distance, it.startTime) }
        }
    }

    /** Dialog dismissed — leaves the trip untouched (still persisted, just not displayed) and stays IDLE. */
    fun handleStrangerSessionDismiss() {
        setState { copy(activeSheet = TrackSheet.NONE, activeStrangerSession = null) }
    }

    /**
     * Wave-4 §2.1: a session was picked from [com.mileway.feature.tracking.ui.sheets.MultiSessionRestoreSheet]
     * (shown instead of the single-session [TrackSheet.SESSION_RESTORE] flow when more than one
     * restorable session exists). Reuses the same restore path as [handleStrangerSessionResume].
     */
    fun handleMultiSessionResume(routeId: String) {
        viewModelScope.launch {
            trackRepo.getByRouteId(routeId)?.let { track ->
                trackingController.start(track.routeId)
                restoreTrackIntoState(track.routeId, track.distance, track.startTime)
            }
        }
    }

    fun selectVehicle(vehicle: ApprovedVehicle) {
        setState { copy(selectedVehicle = vehicle) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun startTracking() {
        val vehicle = currentState.selectedVehicle ?: return
        val routeId = Uuid.random().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val dt =
            kotlin.time.Instant.fromEpochMilliseconds(now)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        val mon = dt.month.name.take(3).let { it[0].uppercase() + it.substring(1).lowercase() }
        val hhmm = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        viewModelScope.launch {
            // P3.3: stamp the real signed-in identity's ownership pointer instead of a hardcoded
            // "EMP001" literal, so cross-account isolation (AccountBinding) has something real to
            // match against. P7.3: while acting on behalf of a reportee, the effective identity is
            // the delegate's, so the trip isolates to the delegate, not the manager's base account.
            val session = sessionSource.sessionState.first()
            val stampIdentity =
                effectiveSignedInIdentity(null, session, delegationSource.delegationState.first())
            // P11.3: if a head-home destination is active for this account, auto-classify the trip
            // toward it by stamping the destination address as the trip's destinationTag.
            val destinationTag =
                destinationModeRepo?.activeTag(
                    activeAccountSource.activeAccountId.first() ?: DESTINATION_GUEST_KEY,
                )
            val track =
                SavedTrack(
                    routeId = routeId,
                    name = "Journey ${dt.dayOfMonth} $mon $hhmm",
                    startLatitude = 0.0, startLongitude = 0.0,
                    endLatitude = 0.0, endLongitude = 0.0,
                    pausedLatitude = 0.0, pausedLongitude = 0.0,
                    startTime = now, endTime = -1L,
                    distance = 0.0, duration = 0L,
                    selectedVehicleType = vehicle.vehicleKey ?: "",
                    vehiclePricing = vehicle.vehiclePricing ?: 0.0,
                    createdAt = now, startedAtTimestamp = now,
                    startedByEmployeeCode = stampIdentity.employeeCode ?: "EMP001",
                    startedByAccountEmail = stampIdentity.accountEmail.orEmpty(),
                    startedByTenant = stampIdentity.tenant,
                    destinationTag = destinationTag,
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
                    val amount =
                        reimbursableAmount(
                            ratePerKm = pricing,
                            gpsDistanceKm = km,
                            odometerDistanceKm = track.odometerDistance / 1000.0,
                            viaOdometer = currentState.config.calculateExpenseViaOdometer,
                        )
                    setState { copy(distanceKm = km, durationMs = track.duration, reimbursableAmount = amount) }
                }
                .launchIn(viewModelScope)
    }

    fun pauseTracking(reason: String? = null) {
        val routeId = currentState.currentRouteId
        routeId?.let { trackingController.pause(it) }
        setState { copy(phase = TrackMilesPhase.PAUSED, pauseReason = reason) }

        if (routeId != null && hardwareEventRepo != null) {
            val event = buildPauseHardwareEvent(routeId, reason, Clock.System.now().toEpochMilliseconds())
            viewModelScope.launch { hardwareEventRepo.insert(event) }
        }
    }

    fun resumeTracking() {
        currentState.currentRouteId?.let { trackingController.resume(it) }
        setState { copy(phase = TrackMilesPhase.TRACKING) }
    }

    fun stopTracking() {
        currentState.currentRouteId?.let { trackingController.stop(it) }
        liveObserveJob?.cancel()
        setState { copy(phase = TrackMilesPhase.STOPPED, endTime = Clock.System.now().toEpochMilliseconds()) }
    }

    fun updateDistance(km: Double) {
        val pricing = currentState.selectedVehicle?.vehiclePricing ?: 0.0
        setState { copy(distanceKm = km, reimbursableAmount = reimbursableAmount(pricing, km)) }
    }

    fun discardTracking() {
        currentState.currentRouteId?.let { trackingController.stop(it) }
        liveObserveJob?.cancel()
        bearingObserveJob?.cancel()
        setState { TrackMilesUiState(config = config, vehicles = vehicles, selectedVehicle = selectedVehicle) }
    }

    // ── P-C.5: session-restore sheet ─────────────────────────────────────────

    private fun observeReconciliationResult() {
        reconciliationHolder?.outcome
            ?.onEach { outcome ->
                if (outcome is SessionReconciliationPolicy.Outcome.NeedsDecision) {
                    val config =
                        RecoverySheetConfig(
                            token = outcome.token,
                            distanceKm = outcome.session.distance / 1_000.0,
                            durationMs = Clock.System.now().toEpochMilliseconds() - outcome.session.startTime,
                            interruptReason = outcome.reason,
                        )
                    setState { copy(activeSheet = TrackSheet.SESSION_RESTORE, activeRecovery = config) }
                    reconciliationHolder.consume()
                }
            }
            ?.launchIn(viewModelScope)
    }

    fun handleRecoveryResume() {
        val config = currentState.activeRecovery ?: return
        trackingController.start(config.token)
        setState { copy(activeSheet = TrackSheet.NONE, activeRecovery = null, phase = TrackMilesPhase.TRACKING, currentRouteId = config.token) }
    }

    fun handleRecoverySaveFinish() {
        val config = currentState.activeRecovery ?: return
        viewModelScope.launch {
            trackRepo.getByRouteId(config.token)?.let { track ->
                trackRepo.update(track.copy(isCompleted = true, endTime = Clock.System.now().toEpochMilliseconds()))
            }
        }
        setState { copy(activeSheet = TrackSheet.NONE, activeRecovery = null) }
    }

    fun handleRecoveryDiscard() {
        val config = currentState.activeRecovery ?: return
        viewModelScope.launch {
            trackingController.stop(config.token)
        }
        setState { copy(activeSheet = TrackSheet.NONE, activeRecovery = null) }
    }

    override fun onCleared() {
        liveObserveJob?.cancel()
        sessionObserveJob?.cancel()
        bearingObserveJob?.cancel()
        trackingStateObserveJob?.cancel()
        super.onCleared()
    }

    private companion object {
        /** KMP-safe 1-decimal-place double formatter (no String.format, works on all targets). */
        fun Double.fmt1(): String {
            val scaled = (this * 10).roundToLong()
            val intPart = scaled / 10
            val fracPart = (scaled % 10).let { if (it < 0) -it else it }
            return "$intPart.$fracPart"
        }

        /** Great-circle initial bearing (degrees, 0–360) between two fixes. */
        fun headingBetween(
            a: com.mileway.core.data.model.db.LocationData,
            b: com.mileway.core.data.model.db.LocationData,
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

/**
 * Builds the [HardwareEvent] persisted when a trip is paused: the reason (quick pick or "Other"
 * free-text) rides in the event's existing `metadata` field — no schema change needed.
 */
internal fun buildPauseHardwareEvent(
    routeId: String,
    reason: String?,
    timeMillis: Long,
): HardwareEvent =
    HardwareEvent(
        token = routeId,
        eventType = EventType.TRACKING_PAUSED,
        event = "Tracking paused",
        time = timeMillis,
        audience = EventAudience.USER,
        metadata = if (!reason.isNullOrBlank()) "Reason: $reason" else null,
    )
