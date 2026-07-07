package com.mileway.feature.logging.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.pad2
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.components.pickers.WheelTimePickerDialog
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.repository.LogMilesFrequentRoute
import com.mileway.feature.logging.ui.components.MapPreviewCard
import com.mileway.feature.logging.ui.components.StepHeaderCard
import com.mileway.feature.logging.ui.components.TapFieldRow
import com.mileway.feature.logging.ui.components.TravelledLocationsActions
import com.mileway.feature.logging.ui.components.TravelledLocationsCard
import com.mileway.feature.logging.ui.dialog.VerifyDistanceDialog
import com.mileway.feature.logging.ui.model.LocationEntry
import com.mileway.feature.logging.ui.sheets.LocationSearchActions
import com.mileway.feature.logging.ui.sheets.LocationSearchSheet
import com.mileway.feature.logging.ui.sheets.OdometerCaptureSheet
import com.mileway.feature.logging.ui.sheets.VehiclePickerSheet
import com.mileway.feature.logging.viewmodel.LogMilesAction
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.mileway.feature.logging.viewmodel.SearchLocationAction
import com.mileway.feature.logging.viewmodel.SearchLocationEffect
import com.mileway.feature.logging.viewmodel.SearchLocationViewModel
import com.mileway.feature.tracking.ui.components.StaticPolylineThumbnail
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Bottom padding so Step 1 content floats above the ~100dp bubble bottom bar. */
private val HomeBottomPadding = 160.dp

/** What a triggered location sheet should do with the picked entry. */
private sealed interface LocationTarget {
    /** Append a new stop to the end of the itinerary. */
    data object Append : LocationTarget

    /** Insert a stop immediately after [index]. */
    data class InsertAfter(val index: Int) : LocationTarget

    /** Replace the place backing the stop with [stopId]. */
    data class Edit(val stopId: Long) : LocationTarget
}

/**
 * Step 1 of the Log Miles flow and the top-level "Log Miles" tab.
 *
 * Collects the journey basics (date, completion time, vehicle), a decorative map
 * preview, and the travelled-locations itinerary with live distance/pricing. The
 * "Next" button advances to Step 2 once there are at least two stops and a vehicle.
 *
 * This screen is the canonical entry ([com.mileway.feature.logging.ui.navigation.LoggingRoutes.HOME]).
 * It is a top-level tab, so its scrolling content carries [HomeBottomPadding] to
 * clear the shell's bubble bottom bar.
 *
 * @param viewModel  shared flow ViewModel (resolved via Koin)
 * @param onNext     navigate to Step 2
 * @param onOpenHistory navigate to the History screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMilesScreen(
    viewModel: LogMilesViewModel = koinViewModel(),
    demoSettings: DemoSettingsRepository = koinInject(),
    onNext: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // P5.3: mirrors the persisted local per-tenant flag into LogMilesUiState, same
    // collectAsState-at-the-screen pattern TrackingNavigation.kt uses for demoSettings.settings
    // (a ViewModel-init collect against a relaxed-mockk-backed repo crashes the screenshot Koin
    // graph — see memory "screenshot Koin needs deterministic fakes").
    val demoSettingsState by demoSettings.settings.collectAsState(initial = DemoSettings())
    LaunchedEffect(demoSettingsState.logMilesOdometerCaptureEnabled) {
        viewModel.onAction(LogMilesAction.SetOdometerCaptureEnabled(demoSettingsState.logMilesOdometerCaptureEnabled))
    }

    // Local UI-only state for overlays.
    var locationTarget by remember { mutableStateOf<LocationTarget?>(null) }
    var showVehicleSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showVerifyDistance by remember { mutableStateOf(false) }
    var odometerSheetPurpose by remember { mutableStateOf<OdometerPurpose?>(null) }
    // VIII.1: entrance visibility
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    /** Route a picked entry to the correct VM action based on the active target. */
    fun handlePick(entry: LocationEntry) {
        when (val target = locationTarget) {
            LocationTarget.Append -> viewModel.onAction(LogMilesAction.AddStop(entry))
            is LocationTarget.InsertAfter ->
                viewModel.onAction(LogMilesAction.InsertStopAfter(target.index, entry))
            is LocationTarget.Edit -> viewModel.onAction(LogMilesAction.EditStop(target.stopId, entry))
            null -> Unit
        }
        locationTarget = null
    }

    // Location switching sheet: its own MVI ViewModel (recents/favorites/saved + current location).
    val searchViewModel: SearchLocationViewModel = koinViewModel()
    val searchState by searchViewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        searchViewModel.effect.collect { effect ->
            when (effect) {
                is SearchLocationEffect.Picked -> handlePick(effect.entry)
            }
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Log Miles",
                subtitle = "Fill out the details given below",
                depth = NavigationDepth.ROOT,
                titleIcon = Icons.Filled.DirectionsCar,
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Log Miles history")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedVisibility(
            visible = contentVisible,
            enter =
                slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(280)) +
                    fadeIn(animationSpec = tween(280)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = DesignTokens.Spacing.l)
                        .padding(top = DesignTokens.Spacing.l, bottom = HomeBottomPadding),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                StepHeaderCard(
                    title = "Step 1 of 2",
                    subtitle = "Add travelled locations and basics",
                )

                // Wave 3: one-tap retrace — pre-fill the itinerary from a cached frequent route
                // instead of re-adding every stop by hand. Only offered before any stop is picked.
                if (uiState.stops.isEmpty() && uiState.frequentRoutes.isNotEmpty()) {
                    FrequentRoutesRow(
                        routes = uiState.frequentRoutes,
                        onRetrace = { viewModel.onAction(LogMilesAction.RetraceRoute(it)) },
                    )
                }

                if (uiState.stops.size >= 2) {
                    StaticPolylineThumbnail(
                        latLngs = uiState.stops.map { it.entry.lat to it.entry.lng },
                    )
                } else {
                    MapPreviewCard(stopCount = uiState.stops.size)
                }

                TapFieldRow(
                    label = "Journey Date",
                    value =
                        uiState.journeyDateMillis?.let { DateUtils.epochToDisplayDate(it) }
                            ?: "Select journey date",
                    isPlaceholder = uiState.journeyDateMillis == null,
                    leadingIcon = Icons.Filled.CalendarMonth,
                    trailingIcon = Icons.Filled.CalendarMonth,
                    onClick = { showDatePicker = true },
                )

                TapFieldRow(
                    label = "Journey Completion Time",
                    value = uiState.journeyTimeMinutes?.let { formatMinutes(it) } ?: "Select time",
                    isPlaceholder = uiState.journeyTimeMinutes == null,
                    leadingIcon = Icons.Filled.Schedule,
                    trailingIcon = Icons.Filled.Schedule,
                    onClick = { showTimePicker = true },
                )

                TapFieldRow(
                    label = "Vehicle Type",
                    value = uiState.selectedVehicle?.vehicleName ?: "Select vehicle type",
                    isPlaceholder = uiState.selectedVehicle == null,
                    leadingIcon = Icons.Filled.DirectionsCar,
                    onClick = { showVehicleSheet = true },
                )

                // P5.3: odometer capture, gated behind the local per-tenant flag.
                if (uiState.odometerCaptureEnabled) {
                    TapFieldRow(
                        label = "Start Odometer Reading",
                        value = uiState.odometerStart?.let { "${it.reading} km" } ?: "Capture start reading",
                        isPlaceholder = uiState.odometerStart == null,
                        leadingIcon = Icons.Filled.Speed,
                        onClick = { odometerSheetPurpose = OdometerPurpose.START },
                    )
                    TapFieldRow(
                        label = "End Odometer Reading",
                        value = uiState.odometerEnd?.let { "${it.reading} km" } ?: "Capture end reading",
                        isPlaceholder = uiState.odometerEnd == null,
                        leadingIcon = Icons.Filled.Speed,
                        onClick = { odometerSheetPurpose = OdometerPurpose.END },
                    )
                    uiState.odometerValidationError?.let { error ->
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                TravelledLocationsCard(
                    stops = uiState.stops,
                    totalDistanceKm = uiState.distanceKm,
                    pricePerKm = uiState.pricePerKm,
                    amount = uiState.reimbursableAmount,
                    isRoundTrip = uiState.isRoundTrip,
                    actions =
                        TravelledLocationsActions(
                            onEdit = { stopId -> locationTarget = LocationTarget.Edit(stopId) },
                            onRemove = { viewModel.onAction(LogMilesAction.RemoveStop(it)) },
                            onMoveUp = { viewModel.onAction(LogMilesAction.MoveStopUp(it)) },
                            onMoveDown = { viewModel.onAction(LogMilesAction.MoveStopDown(it)) },
                            onInsertAfter = { index -> locationTarget = LocationTarget.InsertAfter(index) },
                            onToggleRoundTrip = { viewModel.onAction(LogMilesAction.SetRoundTrip(it)) },
                            onAddLocation = { locationTarget = LocationTarget.Append },
                            onUseCurrent = { locationTarget = LocationTarget.Append },
                            onVerifyDistance = { showVerifyDistance = true },
                        ),
                )

                // Save-as-draft toggle.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.l),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Save as draft",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Store step 1 and finish later",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.saveAsDraft,
                            onCheckedChange = { viewModel.onAction(LogMilesAction.SetSaveAsDraft(it)) },
                        )
                    }
                }

                Text(
                    text =
                        if (uiState.odometerCaptureEnabled) {
                            "You can save a draft after Step 1. Start and end odometer readings are required " +
                                "before continuing to Step 2."
                        } else {
                            "You can save a draft after Step 1. Odometer details can be completed later."
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        if (uiState.saveAsDraft) viewModel.onAction(LogMilesAction.SaveDraft)
                        onNext()
                    },
                    enabled = uiState.canProceedToStep2,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = DesignTokens.Shape.roundedMd,
                ) {
                    Text("Next")
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                    )
                }
            }
        } // AnimatedVisibility
    }

    // ── Overlays ───────────────────────────────────────────────────────────────

    if (locationTarget != null) {
        LocationSearchSheet(
            query = searchState.query,
            results = searchState.results,
            recent = searchState.recent,
            favorites = searchState.favorites,
            saved = searchState.saved,
            favoriteNames = searchState.favoriteNames,
            currentLocation = searchState.currentLocation,
            isLoadingCurrent = searchState.isLoadingCurrent,
            actions =
                LocationSearchActions(
                    onQueryChange = { searchViewModel.onAction(SearchLocationAction.QueryChanged(it)) },
                    onPick = { searchViewModel.onAction(SearchLocationAction.Select(it)) },
                    onToggleFavorite = { searchViewModel.onAction(SearchLocationAction.ToggleFavorite(it)) },
                    onSaveAs = { entry, label -> searchViewModel.onAction(SearchLocationAction.SaveAs(entry, label)) },
                    onRemoveRecent = { searchViewModel.onAction(SearchLocationAction.RemoveRecent(it)) },
                    onRemoveSaved = { searchViewModel.onAction(SearchLocationAction.RemoveSaved(it)) },
                    onUseCurrent = { searchViewModel.onAction(SearchLocationAction.UseCurrentLocation) },
                    onClearRecent = { searchViewModel.onAction(SearchLocationAction.ClearRecent) },
                ),
            onDismiss = { locationTarget = null },
        )
    }

    if (showVehicleSheet) {
        VehiclePickerSheet(
            vehicles = uiState.vehicles,
            onSelect = {
                viewModel.onAction(LogMilesAction.SelectVehicle(it))
                showVehicleSheet = false
            },
            onDismiss = { showVehicleSheet = false },
        )
    }

    if (showDatePicker) {
        WheelDatePickerDialog(
            initialDateMillis = uiState.journeyDateMillis,
            title = "Journey Date",
            onConfirm = {
                viewModel.onAction(LogMilesAction.SetJourneyDate(it))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        WheelTimePickerDialog(
            initialMinutes = uiState.journeyTimeMinutes ?: 9 * 60,
            title = "Journey Completion Time",
            onConfirm = { hour, minute ->
                viewModel.onAction(LogMilesAction.SetJourneyTime(hour, minute))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showVerifyDistance) {
        VerifyDistanceDialog(
            calculatedKm = uiState.calculatedDistanceKm,
            currentKm = uiState.distanceKm,
            onSave = {
                viewModel.onAction(LogMilesAction.OverrideDistance(it))
                showVerifyDistance = false
            },
            onDismiss = { showVerifyDistance = false },
        )
    }

    odometerSheetPurpose?.let { purpose ->
        OdometerCaptureSheet(
            purpose = purpose,
            existing = if (purpose == OdometerPurpose.START) uiState.odometerStart else uiState.odometerEnd,
            onCaptured = {
                viewModel.onAction(LogMilesAction.CaptureOdometerReading(it))
                odometerSheetPurpose = null
            },
            onDismiss = { odometerSheetPurpose = null },
        )
    }
}

/**
 * Wave 3 one-tap retrace: a horizontal row of cached [com.mileway.feature.logging.repository
 * .LogMilesFrequentRoute] chips, most-used first. Tapping one dispatches [LogMilesAction
 * .RetraceRoute] to pre-fill the itinerary instead of re-adding every stop by hand.
 */
@Composable
private fun FrequentRoutesRow(
    routes: List<LogMilesFrequentRoute>,
    onRetrace: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        Text(
            "Frequent routes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            items(routes, key = { it.routeKey }) { route ->
                AssistChip(
                    onClick = { onRetrace(route.routeKey) },
                    label = {
                        val from = route.stops.firstOrNull()?.entry?.name.orEmpty()
                        val to = route.stops.lastOrNull()?.entry?.name.orEmpty()
                        Text("$from → $to")
                    },
                )
            }
        }
    }
}

/** Format minutes-since-midnight as a 12-hour clock string (e.g. "1:25 PM"). */
private fun formatMinutes(totalMinutes: Int): String {
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 =
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    return "$hour12:${minute.pad2()} $amPm"
}
