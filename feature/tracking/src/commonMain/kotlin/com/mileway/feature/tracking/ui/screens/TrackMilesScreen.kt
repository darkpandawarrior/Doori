package com.mileway.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.core.platform.AppPermission
import com.mileway.core.platform.OemBatteryHints
import com.mileway.core.platform.PermissionOnboardingFlow
import com.mileway.core.platform.PermissionsProvider
import com.mileway.core.platform.currentDeviceManufacturer
import com.mileway.core.ui.components.topbar.TrackingStatus
import com.mileway.core.ui.components.topbar.TrackingTopBar
import com.mileway.core.ui.components.tracking.ActivitySegment
import com.mileway.core.ui.components.tracking.ActivityType
import com.mileway.core.ui.components.tracking.CompactSystemStatusIndicator
import com.mileway.core.ui.components.tracking.ExpandableStatsCard
import com.mileway.core.ui.components.tracking.GaugeMode
import com.mileway.core.ui.components.tracking.GaugeSignal
import com.mileway.core.ui.components.tracking.HeroTrackingCard
import com.mileway.core.ui.components.tracking.QuickAction
import com.mileway.core.ui.components.tracking.StatItem
import com.mileway.core.ui.components.tracking.StatusChip
import com.mileway.core.ui.components.tracking.StatusLevel
import com.mileway.core.ui.components.tracking.SystemStatusBanner
import com.mileway.core.ui.components.tracking.ThreeButtonFabSystem
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.tracking.ui.sheets.CenterOption
import com.mileway.feature.tracking.ui.sheets.JourneyConsentSheet
import com.mileway.feature.tracking.ui.sheets.JourneyGuideSheet
import com.mileway.feature.tracking.ui.sheets.JourneyGuideState
import com.mileway.feature.tracking.ui.sheets.MultiSessionRestoreSheet
import com.mileway.feature.tracking.ui.sheets.PauseReasonSheet
import com.mileway.feature.tracking.ui.sheets.PermissionOnboardingSheet
import com.mileway.feature.tracking.ui.sheets.ResumeTrackingSheet
import com.mileway.feature.tracking.ui.sheets.VehicleOption
import com.mileway.feature.tracking.ui.sheets.VehiclePickerSheet
import com.mileway.feature.tracking.ui.sheets.VendorPickerSheet
import com.mileway.feature.tracking.viewmodel.CheckInAction
import com.mileway.feature.tracking.viewmodel.CheckInViewModel
import com.mileway.feature.tracking.viewmodel.HeroGaugeMode
import com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesAction
import com.mileway.feature.tracking.viewmodel.TrackMilesPhase
import com.mileway.feature.tracking.viewmodel.TrackMilesUiState
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.feature.tracking.viewmodel.TrackSheet
import com.mileway.feature.tracking.viewmodel.TrackSignal
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Quick-action ids dispatched from the FAB grid. */
private object Qa {
    const val MAP = "map"
    const val CHECK_IN = "check_in"
    const val MANUAL_CHECK_IN = "manual_check_in"
    const val CENTERS = "centers"
    const val HISTORY = "history"
    const val DATA = "data"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val DISCARD = "discard"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMilesScreen(
    onStop: (id: String, distKm: Double, vehicleKey: String, startTime: Long, endTime: Long) -> Unit,
    onOpenMap: () -> Unit,
    onOpenHwEvents: () -> Unit,
    onOpenCheckInHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToGeoCheckIn: () -> Unit = {},
    onNavigateToManualCheckIn: () -> Unit = {},
    viewModel: TrackMilesViewModel = koinViewModel(),
    checkInViewModel: CheckInViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val checkInUiState by checkInViewModel.state.collectAsState()
    val permissionsProvider = koinInject<PermissionsProvider>()
    val onboardingFlow = remember(permissionsProvider) { PermissionOnboardingFlow(permissionsProvider) }
    val onboardingState by onboardingFlow.state.collectAsState()
    val oemHint = remember { currentDeviceManufacturer()?.let(OemBatteryHints::hintFor) }
    val scope = rememberCoroutineScope()
    val isActive = uiState.phase == TrackMilesPhase.TRACKING || uiState.phase == TrackMilesPhase.PAUSED
    val isPaused = uiState.phase == TrackMilesPhase.PAUSED
    var statsExpanded by remember { mutableStateOf(true) }

    // Once tracking is active, skip past any onboarding tier already granted (e.g. from a previous
    // session) so the sheet only ever prompts for what's genuinely still outstanding.
    LaunchedEffect(isActive) {
        if (isActive) onboardingFlow.skipAlreadyGranted()
    }

    // When tracking stops, hand off to the submission flow.
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == TrackMilesPhase.STOPPED) {
            uiState.currentRouteId?.let { routeId ->
                onStop(
                    routeId,
                    uiState.distanceKm,
                    uiState.selectedVehicle?.vehicleKey ?: "",
                    uiState.startTime,
                    uiState.endTime,
                )
            }
        }
    }

    // Surface check-in success as a toast.
    LaunchedEffect(checkInUiState.checkInSuccess) {
        if (checkInUiState.checkInSuccess) {
            Toasts.show(
                scenario = Toasts.ToastScenario.Success,
                title = "Checked In",
                description = checkInUiState.successMessage.ifBlank { "Check-in recorded." },
            )
            checkInViewModel.onAction(CheckInAction.AcknowledgeSuccess)
        }
    }

    val requestStartTracking: () -> Unit = {
        // A.3/A.4: collapse any open coach-mark (Journey Guide) BEFORE the permission prompt or
        // consent sheet appears, so a modal never stacks over the start/consent flow.
        viewModel.onAction(TrackMilesAction.DismissSheet)
        scope.launch {
            // Wave-3 tiered onboarding: resume re-check first (covers grants made in system settings
            // since the flow was last shown), then walk the ladder only for the required tier inline —
            // optional tiers (background location, notifications, activity recognition) are offered
            // through the sheet below and never block tracking from starting.
            onboardingFlow.recheck()
            if (!onboardingFlow.state.value.requiredSatisfied) {
                onboardingFlow.requestCurrent()
            }
            viewModel.onAction(TrackMilesAction.RequestStartTracking)
        }
        Unit
    }

    val trackingStatus =
        when {
            isPaused -> TrackingStatus.PAUSED
            isActive -> TrackingStatus.TRACKING
            else -> TrackingStatus.IDLE
        }
    Scaffold(
        topBar = {
            TrackingTopBar(
                title = "Track Miles",
                status = trackingStatus,
                actions = {
                    // Right-side sync / network-status indicator, only meaningful while a journey
                    // is live; communicates queued vs synced points without a coloured chrome band.
                    if (isActive) {
                        SyncStatusAction(unsyncedPoints = uiState.unsyncedPoints)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(DesignTokens.Spacing.l)
                        // Leave room so the pinned control cluster never hides the last card.
                        .padding(bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                CurrentLocationPill(
                    label = uiState.currentLocationLabel,
                    coordinates = uiState.currentLocationCoordinates,
                )

                HeroTrackingCard(
                    distanceText = "${(uiState.distanceKm * 100).toLong() / 100.0}",
                    durationText = formatElapsed(liveElapsedMs(uiState)),
                    vehicleName = uiState.selectedVehicle?.vehicleName,
                    bearingDegrees = uiState.bearingDegrees,
                    speedKmh = if (isActive) uiState.speedKmh.toFloat() else null,
                    signalQuality = uiState.signal.toGauge(),
                    segments = uiState.activitySegments(),
                    gaugeMode = uiState.gaugeMode.toGauge(),
                    onToggleMode = { viewModel.onAction(TrackMilesAction.ToggleGaugeMode) },
                    isActive = isActive,
                    isPaused = isPaused,
                    historyCount = uiState.pointsLabel,
                    trackingActivity = uiState.trackingActivity,
                    vehicleIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.height(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onVehicleClick =
                        if (uiState.phase == TrackMilesPhase.IDLE) {
                            { viewModel.onAction(TrackMilesAction.OpenVehiclePicker) }
                        } else {
                            null
                        },
                    pauseReason = uiState.pauseReason,
                )

                // Weekly summary pill, shown only when idle, below the hero card.
                if (!isActive && uiState.weekSummaryText.isNotEmpty()) {
                    WeeklySummaryPill(text = uiState.weekSummaryText)
                }

                // Live system-status chips while active; a calm "All systems OK" banner otherwise.
                if (isActive) {
                    CompactSystemStatusIndicator(chips = uiState.statusChips())
                }
                SystemStatusBanner(allOk = uiState.error == null, message = uiState.error ?: "All systems OK")

                if (isActive) {
                    ExpandableStatsCard(
                        stats = uiState.statItems(),
                        expanded = statsExpanded,
                        onToggle = { statsExpanded = !statsExpanded },
                    )
                }

                // Journey Guide text link, tappable hint shown when idle.
                if (!isActive) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.onAction(TrackMilesAction.OpenJourneyGuide) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Journey Guide →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Pinned control cluster, always reachable, never scrolls away.
            ThreeButtonFabSystem(
                isActive = isActive,
                isPaused = isPaused,
                actions = quickActions,
                onHero = {
                    if (isActive) {
                        viewModel.onAction(TrackMilesAction.StopTracking)
                    } else {
                        // A.3: the hero FAB is the single primary action. Tapping START goes
                        // straight to permission → (consent if configured) → tracking, with no
                        // mandatory Journey Guide coach-mark in the way. Setup (vehicle / odometer)
                        // stays available via the optional "Journey Guide →" link below.
                        requestStartTracking()
                    }
                },
                onPauseResume = {
                    if (isPaused) {
                        viewModel.onAction(TrackMilesAction.OpenResumeSheet)
                    } else {
                        viewModel.onAction(TrackMilesAction.OpenPauseSheet)
                    }
                },
                onAction = { id ->
                    when (id) {
                        Qa.MAP -> onOpenMap()
                        Qa.HISTORY -> onOpenCheckInHistory()
                        Qa.DATA -> onOpenHwEvents()
                        Qa.CHECK_IN -> onNavigateToGeoCheckIn()
                        Qa.MANUAL_CHECK_IN -> onNavigateToManualCheckIn()
                        Qa.CENTERS -> viewModel.onAction(TrackMilesAction.OpenVendorPicker)
                        Qa.SAVED -> onOpenHwEvents()
                        Qa.SETTINGS -> onOpenSettings()
                        Qa.DISCARD -> viewModel.onAction(TrackMilesAction.DiscardTracking)
                    }
                },
                showGeoCheckIn = isActive && uiState.config.geoCheckInEnabled,
                onGeoCheckIn = onNavigateToGeoCheckIn,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // ── Start-flow & control sheets ─────────────────────────────────────────────
    when (uiState.activeSheet) {
        TrackSheet.JOURNEY_GUIDE ->
            JourneyGuideSheet(
                state =
                    JourneyGuideState(
                        step = uiState.journeyStep,
                        vehicleName = uiState.selectedVehicle?.vehicleName,
                        vehicleRatePerKm = uiState.selectedVehicle?.vehiclePricing,
                        startOdometer = uiState.startOdometer,
                        draftEnabled = uiState.draftEnabled,
                        requiresOdometer = uiState.config.isOdometerMandatory,
                    ),
                onPickVehicle = { viewModel.onAction(TrackMilesAction.OpenVehiclePicker) },
                onCaptureOdometer = { viewModel.onAction(TrackMilesAction.CaptureStartOdometer) },
                onToggleDraft = { viewModel.onAction(TrackMilesAction.ToggleDraft(it)) },
                onStartTracking = requestStartTracking,
                onDismiss = { viewModel.onAction(TrackMilesAction.DismissSheet) },
            )

        TrackSheet.VEHICLE_PICKER ->
            VehiclePickerSheet(
                vehicles =
                    uiState.vehicles.map {
                        VehicleOption(
                            key = it.vehicleKey ?: it.vehicleName.orEmpty(),
                            name = it.vehicleName.orEmpty(),
                            ratePerKm = it.vehiclePricing ?: 0.0,
                            icon = Icons.Filled.DirectionsCar,
                        )
                    },
                query = uiState.vehicleQuery,
                onQueryChange = { viewModel.onAction(TrackMilesAction.SetVehicleQuery(it)) },
                onSelect = { viewModel.onAction(TrackMilesAction.PickVehicle(it)) },
                onDismiss = { viewModel.onAction(TrackMilesAction.OpenJourneyGuide) },
            )

        TrackSheet.VENDOR_PICKER ->
            VendorPickerSheet(
                centers = uiState.centers.map { CenterOption(it.id, it.name, it.type) },
                query = uiState.vendorQuery,
                onQueryChange = { viewModel.onAction(TrackMilesAction.SetVendorQuery(it)) },
                onSelect = { viewModel.onAction(TrackMilesAction.PickVendor(it)) },
                onOpenMaps = { /* maps deep-link is out of scope for the offline demo */ },
                onDismiss = { viewModel.onAction(TrackMilesAction.DismissSheet) },
            )

        TrackSheet.PAUSE ->
            PauseReasonSheet(
                timestamp = "now",
                selectedReason = uiState.pauseSelectedReason,
                customReason = uiState.pauseCustomReason,
                onSelectReason = { viewModel.onAction(TrackMilesAction.SetPauseReason(it)) },
                onCustomReason = { viewModel.onAction(TrackMilesAction.SetPauseCustomReason(it)) },
                onConfirm = { viewModel.onAction(TrackMilesAction.ConfirmPause(it)) },
                onCancel = { viewModel.onAction(TrackMilesAction.DismissSheet) },
            )

        TrackSheet.RESUME ->
            ResumeTrackingSheet(
                pauseReason = uiState.pauseReason,
                resumeNotes = uiState.resumeNotes,
                onNotesChange = { viewModel.onAction(TrackMilesAction.SetResumeNotes(it)) },
                onResume = { viewModel.onAction(TrackMilesAction.ConfirmResume) },
                onCancel = { viewModel.onAction(TrackMilesAction.DismissSheet) },
            )

        TrackSheet.CONSENT ->
            JourneyConsentSheet(
                disclaimer = uiState.journeyDisclaimerOrDefault(),
                onAccept = { viewModel.onAction(TrackMilesAction.AcceptConsentAndStart) },
                onDismiss = { viewModel.onAction(TrackMilesAction.DismissSheet) },
            )

        // P-C.5: session-restore sheet — shown when a prior session was interrupted.
        TrackSheet.SESSION_RESTORE ->
            uiState.activeRecovery?.let { recovery ->
                com.mileway.feature.tracking.ui.sheets.SessionRestoreBottomSheet(
                    config = recovery,
                    onResume = { viewModel.onAction(TrackMilesAction.RecoveryResume) },
                    onSaveFinish = { viewModel.onAction(TrackMilesAction.RecoverySaveFinish) },
                    onDiscard = { viewModel.onAction(TrackMilesAction.RecoveryDiscard) },
                )
            }

        // P3.5: cold-start ownership-mismatch dialog — shown instead of silently restoring
        // another persona's in-progress trip.
        TrackSheet.STRANGER_SESSION ->
            uiState.activeStrangerSession?.let { stranger ->
                com.mileway.feature.tracking.ui.sheets.StrangerSessionSheet(
                    config = stranger,
                    onResume = { viewModel.onAction(TrackMilesAction.StrangerSessionResume) },
                    onDismiss = { viewModel.onAction(TrackMilesAction.StrangerSessionDismiss) },
                )
            }

        TrackSheet.NONE -> Unit
    }

    // Wave-4 §2.1: multi-session restore — shown instead of the single-session SESSION_RESTORE/
    // STRANGER_SESSION sheets above when more than one local restorable session exists. A single
    // session keeps using those existing flows untouched.
    val multiSessionViewModel: MultiSessionRestoreViewModel = koinViewModel()
    val restorableSessions by multiSessionViewModel.sessions.collectAsStateWithLifecycle()
    var multiSessionDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.phase) {
        // Reset once tracking starts, so a future interruption can show the sheet again.
        if (isActive) multiSessionDismissed = false
    }
    if (restorableSessions.size > 1 &&
        !multiSessionDismissed &&
        uiState.activeSheet == TrackSheet.NONE &&
        uiState.phase == TrackMilesPhase.IDLE
    ) {
        MultiSessionRestoreSheet(
            sessions = restorableSessions,
            onResume = { routeId ->
                multiSessionDismissed = true
                viewModel.onAction(TrackMilesAction.MultiSessionResume(routeId))
            },
            onDismiss = { multiSessionDismissed = true },
        )
    }

    // Wave-3 tiered permission onboarding — optional tiers only, offered once tracking is underway so the
    // required-location prompt above never gets blocked behind this. OEM hint is shown only alongside the
    // background-location tier, since that's the one battery managers actually interfere with.
    onboardingState.current?.let { tier ->
        if (onboardingState.requiredSatisfied && isActive) {
            PermissionOnboardingSheet(
                tier = tier,
                oemHint = if (tier.permission == AppPermission.LOCATION_BACKGROUND) oemHint else null,
                onGrant = { scope.launch { onboardingFlow.requestCurrent() } },
                onSkip = { onboardingFlow.skipCurrent() },
            )
        }
    }

    // ── Check-in sheets (unchanged) ─────────────────────────────────────────────
    if (checkInUiState.showManualCheckInSheet) {
        ManualCheckInSheet(
            viewModel = checkInViewModel,
            uiState = checkInUiState,
            onDismiss = { checkInViewModel.onAction(CheckInAction.DismissManualCheckIn) },
        )
    }
    if (checkInUiState.showGeoCheckInSheet) {
        GeoCheckInSheet(
            viewModel = checkInViewModel,
            uiState = checkInUiState,
            currentLat = uiState.currentLat,
            currentLng = uiState.currentLng,
            onDismiss = { checkInViewModel.onAction(CheckInAction.DismissGeoCheckIn) },
        )
    }
    if (checkInUiState.showRadiusWarning) {
        CheckInRadiusWarningSheet(
            viewModel = checkInViewModel,
            uiState = checkInUiState,
            onDismiss = { checkInViewModel.onAction(CheckInAction.DismissRadiusWarning) },
        )
    }
}

// ── State → component mapping ───────────────────────────────────────────────────

private val quickActions =
    listOf(
        QuickAction(Qa.MAP, "Map", Icons.Filled.Map),
        QuickAction(Qa.CHECK_IN, "Check In", Icons.Filled.CheckCircle),
        QuickAction(Qa.MANUAL_CHECK_IN, "Manual Check In", Icons.Filled.PinDrop),
        QuickAction(Qa.CENTERS, "Centers", Icons.Filled.Storefront),
        QuickAction(Qa.HISTORY, "Journey History", Icons.Filled.History),
        QuickAction(Qa.DATA, "Data", Icons.Filled.DataObject),
        QuickAction(Qa.SAVED, "Saved Journeys", Icons.Filled.Bookmark),
        QuickAction(Qa.SETTINGS, "Settings", Icons.Filled.Settings),
        QuickAction(Qa.DISCARD, "Discard", Icons.Filled.DeleteOutline, destructive = true),
    )

private fun TrackSignal.toGauge(): GaugeSignal =
    when (this) {
        TrackSignal.GOOD -> GaugeSignal.GOOD
        TrackSignal.FAIR -> GaugeSignal.FAIR
        TrackSignal.POOR -> GaugeSignal.POOR
    }

private fun HeroGaugeMode.toGauge(): GaugeMode =
    when (this) {
        HeroGaugeMode.COMPASS -> GaugeMode.COMPASS
        HeroGaugeMode.ACTIVITY -> GaugeMode.ACTIVITY
    }

private fun TrackMilesUiState.activitySegments(): List<ActivitySegment> {
    if (phase == TrackMilesPhase.IDLE) return emptyList()
    val type =
        when (trackingActivity.lowercase()) {
            "walking" -> ActivityType.WALKING
            "cycling" -> ActivityType.CYCLING
            "driving", "in_vehicle", "automotive" -> ActivityType.DRIVING
            else -> ActivityType.IDLE
        }
    return listOf(ActivitySegment(type, 1f))
}

private fun TrackMilesUiState.statusChips(): List<StatusChip> =
    buildList {
        add(StatusChip(Icons.Filled.GpsFixed, "GPS", signal.toLevel()))
        add(
            StatusChip(
                Icons.Filled.NetworkCell,
                if (unsyncedPoints > 0) "$unsyncedPoints queued" else "Synced",
                if (unsyncedPoints > 0) StatusLevel.WARN else StatusLevel.OK,
            ),
        )
        add(StatusChip(Icons.Filled.Speed, "${speedKmh.toLong()} km/h", StatusLevel.OK))
        // C.3: live tracking-quality score, spike filtering, and any active system-health issue.
        add(StatusChip(Icons.Filled.Insights, "Quality $qualityScore%", qualityScore.toQualityLevel()))
        if (spikeDistanceM >= 1.0) {
            add(StatusChip(Icons.Filled.Warning, "Spikes ${spikeDistanceM.toLong()} m", StatusLevel.WARN))
        }
        systemFlags.firstIssueLabel()?.let { add(StatusChip(Icons.Filled.Warning, it, StatusLevel.BAD)) }
    }

private fun TrackSignal.toLevel(): StatusLevel =
    when (this) {
        TrackSignal.GOOD -> StatusLevel.OK
        TrackSignal.FAIR -> StatusLevel.WARN
        TrackSignal.POOR -> StatusLevel.BAD
    }

/** C.3: bucket the 0..100 quality score into the chip severity levels. */
private fun Int.toQualityLevel(): StatusLevel =
    when {
        this >= 80 -> StatusLevel.OK
        this >= 50 -> StatusLevel.WARN
        else -> StatusLevel.BAD
    }

/** C.3: the most-severe active system-health issue, as a short chip label (null when healthy). */
private fun TrackingSystemFlags.firstIssueLabel(): String? =
    when {
        gpsDisabled -> "GPS off"
        permissionMissing -> "Permission"
        mockLocationDetected -> "Mock GPS"
        powerSaverOn -> "Power saver"
        batteryOptimized -> "Battery opt"
        else -> null
    }

private fun TrackMilesUiState.statItems(): List<StatItem> =
    buildList {
        add(StatItem("Distance", "${(distanceKm * 100).toLong() / 100.0} km", Icons.Filled.Map))
        add(StatItem("Duration", formatElapsed(liveElapsedMs(this@statItems)), Icons.Filled.Timer))
        add(StatItem("Avg Speed", "${(avgSpeedKmh * 10).toLong() / 10.0} km/h", Icons.Filled.Speed))
        add(StatItem("Points", pointsLabel.toString(), Icons.Filled.GpsFixed))
        add(StatItem("Max Speed", "${(maxSpeedKmh * 10).toLong() / 10.0} km/h", Icons.Filled.Bolt))
        add(StatItem("Activity", trackingActivity, Icons.Filled.DirectionsCar))
        if (pauseReason != null) {
            add(StatItem("Pause Reason", pauseReason, Icons.Filled.Timer))
        }
    }

private fun TrackMilesUiState.journeyDisclaimerOrDefault(): String =
    "By starting this journey you confirm the trip details are accurate and consent to " +
        "location tracking for the duration of the journey."

private fun liveElapsedMs(uiState: TrackMilesUiState): Long =
    when {
        uiState.startTime > 0 && uiState.phase == TrackMilesPhase.TRACKING ->
            (kotlin.time.Clock.System.now().toEpochMilliseconds() - uiState.startTime).coerceAtLeast(uiState.durationMs)
        else -> uiState.durationMs
    }.coerceAtLeast(0L)

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
}

/** Weekly summary pill shown below the hero card when idle. */
@Composable
private fun WeeklySummaryPill(text: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.l,
                    vertical = DesignTokens.Spacing.m,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Right-side top-bar action: a compact sync / network indicator for the live session. Shows the
 * queued-point count in [MilewayColors.warning] when points are waiting to sync, or a calm "Synced"
 * state otherwise. Communicates pipeline health on the right of the bar (no coloured chrome band).
 */
@Composable
private fun SyncStatusAction(unsyncedPoints: Long) {
    val pending = unsyncedPoints > 0
    val tint = if (pending) MilewayColors.warning else MilewayColors.success
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .padding(end = DesignTokens.Spacing.xs)
                .clip(DesignTokens.Shape.chip)
                .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Icon(
            imageVector = if (pending) Icons.Filled.CloudQueue else Icons.Filled.CloudDone,
            contentDescription = if (pending) "$unsyncedPoints points queued to sync" else "All points synced",
            tint = tint,
            modifier = Modifier.size(DesignTokens.IconSize.navigation),
        )
        Text(
            text = if (pending) "$unsyncedPoints" else "Synced",
            style = MaterialTheme.typography.labelSmall.dataStyle(),
            color = tint,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

/**
 * The current-location pill shown above the hero card. Shows the geocoded place name as the
 * primary line, with the raw coordinates as a muted secondary line. When no name is resolved
 * (offline coords-only fallback), the secondary line is suppressed if it would duplicate the label.
 */
@Composable
private fun CurrentLocationPill(
    label: String,
    coordinates: String,
) {
    // Show coords as the muted second line only when they add information beyond the label.
    val showCoords = coordinates.isNotBlank() && coordinates != label
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.l,
                    vertical = DesignTokens.Spacing.m,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
                if (showCoords) {
                    Text(
                        text = coordinates,
                        style = MaterialTheme.typography.labelSmall.dataStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
