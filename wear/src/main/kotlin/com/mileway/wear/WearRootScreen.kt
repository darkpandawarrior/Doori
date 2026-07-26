package com.mileway.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.mileway.wear.theme.WearMilewayTheme
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

/**
 * P2.4/P2.5: the Wear app's single screen surface — dashboard, trip list ([TripListScreen]) and
 * trip detail ([TripDetailScreen], both in `WearTripScreens.kt`) all live behind this one
 * `Composable`, dispatched by [WearRootUiState.screen] (biciradar single-activity pattern:
 * [WearViewModel] never triggers a new Activity, it just swaps [WearScreen]). System back
 * (crown/gesture) is wired to [WearViewModel.onBack] via [BackHandler] so "back returns" per P2.5's
 * acceptance works the same way a real nav stack would, without pulling in `wear-compose-navigation`
 * for a two-level stack this shallow.
 *
 * P2.8: also collects [WearViewModel.ongoingActivityState] and drives [TrackingOngoingActivity]
 * from it in a [LaunchedEffect] keyed on [OngoingActivityUi.isLive] — posting a fresh notification
 * on every live update while tracking is active, cancelling it the moment tracking stops (per
 * P2.8's acceptance: "starting tracking shows the ongoing notification with live distance; stop
 * clears it").
 */
@Composable
fun WearRootScreen(viewModel: WearViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ongoingActivityState by viewModel.ongoingActivityState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler(enabled = uiState.screen != WearScreen.Dashboard) { viewModel.onBack() }
    LaunchedEffect(ongoingActivityState) {
        if (ongoingActivityState.isLive) {
            TrackingOngoingActivity.post(context, ongoingActivityState.distanceKm)
        } else {
            TrackingOngoingActivity.cancel(context)
        }
    }
    WearMilewayTheme {
        AppScaffold {
            // Rotary (crown/bezel) scrolling needs no wiring here or in `WearTripScreens.kt`:
            // foundation's ScalingLazyColumn applies `requestFocusOnHierarchyActive()` +
            // `rotaryScrollable(...)` itself, defaulting `rotaryScrollableBehavior` to
            // `RotaryScrollableDefaults.behavior(state)` off this shared [listState]. Adding a
            // second `Modifier.rotaryScrollable` would stack a competing focus target — don't.
            val listState = rememberScalingLazyListState()
            ScreenScaffold(scrollState = listState) {
                when (uiState.screen) {
                    WearScreen.Dashboard ->
                        WearDashboard(
                            uiState = uiState,
                            listState = listState,
                            onTripsClick = viewModel::openTripList,
                        )
                    WearScreen.TripList ->
                        TripListScreen(
                            trips = uiState.trips,
                            listState = listState,
                            onTripClick = viewModel::openTripDetail,
                        )
                    WearScreen.TripDetail ->
                        TripDetailScreen(trip = uiState.selectedTrip, listState = listState)
                }
            }
        }
    }
}

@Composable
internal fun WearDashboard(
    uiState: WearRootUiState,
    listState: ScalingLazyListState,
    onTripsClick: () -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item { DashboardHeader() }
        item { TrackingPill(isTracking = uiState.isTracking) }
        item { DistanceCard(label = "TODAY", km = uiState.todayDistanceKm) }
        item { DistanceCard(label = "WEEK", km = uiState.weekDistanceKm) }
        item { WeekGoalCard(uiState = uiState) }
        item { TripsEntryCard(tripCount = uiState.trips.size, onClick = onTripsClick) }
    }
}

@Composable
private fun DashboardHeader() {
    Text(
        text = "Mileway",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Dashboard entry point into [TripListScreen] — a clickable card so the recent-trips count is
 * visible without opening the list, per P2.5's "tapping opens detail" acceptance chain. */
@Composable
private fun TripsEntryCard(
    tripCount: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$tripCount trips, open trip list"
                }
                .padding(CARD_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING_DP.dp),
    ) {
        Text(text = "TRIPS", style = MaterialTheme.typography.labelSmall)
        Text(
            text = tripCount.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** A non-interactive info panel — Wear's `Card` composable is click-only, so a plain surface
 * container is used for read-only dashboard tiles instead (no fake `onClick = {}` semantics).
 * Internal (not private) so `WearTripScreens.kt`'s [TripDateCard][com.mileway.wear.TripDateCard]
 * can reuse the same read-only-panel styling. */
@Composable
internal fun InfoPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(CARD_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING_DP.dp),
    ) {
        content()
    }
}

@Composable
private fun TrackingPill(isTracking: Boolean) {
    InfoPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = if (isTracking) "Tracking status: tracking" else "Tracking status: idle"
                },
    ) {
        Text(
            text = if (isTracking) "TRACKING" else "IDLE",
            style = MaterialTheme.typography.labelMedium,
            color = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Internal (not private) so `WearTripScreens.kt`'s trip-detail distance row can reuse it. */
@Composable
internal fun DistanceCard(
    label: String,
    km: Double,
) {
    InfoPanel(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = "%.1f km".format(km),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun WeekGoalCard(uiState: WearRootUiState) {
    InfoPanel(modifier = Modifier.fillMaxWidth()) {
        Text(text = "WEEK GOAL", style = MaterialTheme.typography.labelSmall)
        CircularProgressIndicator(progress = { uiState.weekGoalProgress })
        Text(
            text = "${(uiState.weekGoalProgress * PERCENT_SCALE).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

internal const val CARD_CORNER_RADIUS_DP = 12
internal const val CARD_PADDING_DP = 8
internal const val CARD_SPACING_DP = 2
private const val PERCENT_SCALE = 100

// UnusedPrivateMember: only called by the Compose/Android Studio preview renderer via reflection
// on the @Preview annotation, never from Kotlin call sites — see WearMilewayThemePreview's identical
// suppression in WearMilewayTheme.kt for the same known detekt false positive.
@Suppress("DEPRECATION", "UnusedPrivateMember")
@Preview(name = "Wear dashboard", device = Devices.WEAR_OS_SMALL_ROUND, showBackground = true)
@Composable
private fun WearDashboardPreview() {
    WearMilewayTheme {
        AppScaffold {
            val listState = rememberScalingLazyListState()
            ScreenScaffold(scrollState = listState) {
                WearDashboard(
                    uiState =
                        WearRootUiState(
                            todayDistanceKm = 12.4,
                            weekDistanceKm = 58.7,
                            isTracking = true,
                            weekGoalKm = 100.0,
                            weekGoalProgress = 0.587f,
                        ),
                    listState = listState,
                    onTripsClick = {},
                )
            }
        }
    }
}
