package com.miletracker.feature.tracking.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.components.tracking.ActivitySegment
import com.miletracker.core.ui.components.tracking.ActivityType
import com.miletracker.core.ui.components.tracking.CompactSystemStatusIndicator
import com.miletracker.core.ui.components.tracking.ExpandableStatsCard
import com.miletracker.core.ui.components.tracking.GaugeMode
import com.miletracker.core.ui.components.tracking.GaugeSignal
import com.miletracker.core.ui.components.tracking.HeroTrackingCard
import com.miletracker.core.ui.components.tracking.QuickAction
import com.miletracker.core.ui.components.tracking.StatItem
import com.miletracker.core.ui.components.tracking.StatusChip
import com.miletracker.core.ui.components.tracking.StatusLevel
import com.miletracker.core.ui.components.tracking.SystemStatusBanner
import com.miletracker.core.ui.components.tracking.ThreeButtonFabSystem
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.MilewayColors
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.tracking.ui.components.StatusBadge
import com.miletracker.feature.tracking.ui.sheets.CenterOption
import com.miletracker.feature.tracking.ui.sheets.JourneyConsentSheet
import com.miletracker.feature.tracking.ui.sheets.JourneyGuideSheet
import com.miletracker.feature.tracking.ui.sheets.JourneyGuideState
import com.miletracker.feature.tracking.ui.sheets.JourneyGuideStep
import com.miletracker.feature.tracking.ui.sheets.PauseReasonSheet
import com.miletracker.feature.tracking.ui.sheets.ResumeTrackingSheet
import com.miletracker.feature.tracking.ui.sheets.VehicleOption
import com.miletracker.feature.tracking.ui.sheets.VehiclePickerSheet
import com.miletracker.feature.tracking.ui.sheets.VendorPickerSheet
import com.miletracker.feature.tracking.viewmodel.CheckInAction
import com.miletracker.feature.tracking.viewmodel.CheckInViewModel
import com.miletracker.feature.tracking.viewmodel.HeroGaugeMode
import com.miletracker.feature.tracking.viewmodel.TrackMilesAction
import com.miletracker.feature.tracking.viewmodel.TrackMilesPhase
import com.miletracker.feature.tracking.viewmodel.TrackMilesUiState
import com.miletracker.feature.tracking.viewmodel.TrackMilesViewModel
import com.miletracker.feature.tracking.viewmodel.TrackSheet
import com.miletracker.feature.tracking.viewmodel.TrackSignal
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
    val context = LocalContext.current
    val isActive = uiState.phase == TrackMilesPhase.TRACKING || uiState.phase == TrackMilesPhase.PAUSED
    val isPaused = uiState.phase == TrackMilesPhase.PAUSED
    var statsExpanded by remember { mutableStateOf(true) }

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

    // Location permission is requested before the foreground service starts; we proceed
    // through the guide regardless of the result (the demo simulates GPS).
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { viewModel.onAction(TrackMilesAction.RequestStartTracking) }
    val requestStartTracking = {
        val granted =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onAction(TrackMilesAction.RequestStartTracking)
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Track Miles",
                depth = DesignTokens.NavigationDepth.ROOT,
                actions = {
                    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant
                    StatusBadge(
                        text =
                            when {
                                isPaused -> "⏸ PAUSED"
                                isActive -> "● ACTIVE"
                                else -> "● Not Tracking"
                            },
                        color =
                            when {
                                isPaused -> MilewayColors.warning
                                isActive -> MilewayColors.success
                                else -> idleColor
                            },
                    )
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
                CurrentLocationPill(label = uiState.currentLocationLabel)

                HeroTrackingCard(
                    distanceText = "%.2f".format(uiState.distanceKm),
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

                // Weekly summary pill — shown only when idle, below the hero card.
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

                // Journey Guide text link — tappable hint shown when idle.
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

            // Pinned control cluster — always reachable, never scrolls away.
            ThreeButtonFabSystem(
                isActive = isActive,
                isPaused = isPaused,
                actions = quickActions,
                onHero = {
                    if (isActive) {
                        viewModel.onAction(TrackMilesAction.StopTracking)
                    } else {
                        viewModel.onAction(TrackMilesAction.OpenJourneyGuide)
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
                        step =
                            when {
                                uiState.selectedVehicle == null -> JourneyGuideStep.VEHICLE
                                else -> JourneyGuideStep.TRACKING
                            },
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

        TrackSheet.NONE -> Unit
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
    listOf(
        StatusChip(Icons.Filled.GpsFixed, "GPS", signal.toLevel()),
        StatusChip(
            Icons.Filled.NetworkCell,
            if (unsyncedPoints > 0) "$unsyncedPoints queued" else "Synced",
            if (unsyncedPoints > 0) StatusLevel.WARN else StatusLevel.OK,
        ),
        StatusChip(Icons.Filled.Speed, "%.0f km/h".format(speedKmh), StatusLevel.OK),
    )

private fun TrackSignal.toLevel(): StatusLevel =
    when (this) {
        TrackSignal.GOOD -> StatusLevel.OK
        TrackSignal.FAIR -> StatusLevel.WARN
        TrackSignal.POOR -> StatusLevel.BAD
    }

private fun TrackMilesUiState.statItems(): List<StatItem> =
    buildList {
        add(StatItem("Distance", "%.2f km".format(distanceKm), Icons.Filled.Map))
        add(StatItem("Duration", formatElapsed(liveElapsedMs(this@statItems)), Icons.Filled.Timer))
        add(StatItem("Avg Speed", "%.1f km/h".format(avgSpeedKmh), Icons.Filled.Speed))
        add(StatItem("Points", pointsLabel.toString(), Icons.Filled.GpsFixed))
        add(StatItem("Max Speed", "%.1f km/h".format(maxSpeedKmh), Icons.Filled.Bolt))
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
            (System.currentTimeMillis() - uiState.startTime).coerceAtLeast(uiState.durationMs)
        else -> uiState.durationMs
    }.coerceAtLeast(0L)

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
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

/** The current-location pill shown above the hero card (mirrors the reference layout). */
@Composable
private fun CurrentLocationPill(label: String) {
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
