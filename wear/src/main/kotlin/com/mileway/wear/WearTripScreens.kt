package com.mileway.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.mileway.wear.theme.WearMilewayTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P2.5: the recent-trips list — [com.mileway.feature.tracking.watch.WatchFacade.recentTrips]
 * mapped through [WearPresentation.toTripListItems]. Tapping a row opens [TripDetailScreen]
 * (dispatched from [WearRootScreen] via [WearScreen.TripDetail]).
 */
@Composable
internal fun TripListScreen(
    trips: List<TripListItemUi>,
    listState: ScalingLazyListState,
    onTripClick: (String) -> Unit,
) {
    if (trips.isEmpty()) {
        EmptyTripsMessage()
        return
    }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item {
            Text(
                text = "RECENT TRIPS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(trips) { trip -> TripRow(trip = trip, onClick = { onTripClick(trip.id) }) }
    }
}

/** P2.5: the trip-detail surface for [trip] — `null` only transiently (e.g. the trip vanished from
 * the underlying repository between list and detail); shows a fallback rather than crashing. */
@Composable
internal fun TripDetailScreen(
    trip: TripListItemUi?,
    listState: ScalingLazyListState,
) {
    if (trip == null) {
        EmptyTripsMessage()
        return
    }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item {
            Text(
                text = trip.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item { DistanceCard(label = "DISTANCE", km = trip.km) }
        item { TripDateCard(endMs = trip.endMs) }
    }
}

@Composable
private fun EmptyTripsMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No trips yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripRow(
    trip: TripListItemUi,
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
                    contentDescription = "${trip.label}, %.1f km".format(trip.km)
                }.padding(CARD_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING_DP.dp),
    ) {
        Text(text = trip.label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "%.1f km".format(trip.km),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TripDateCard(endMs: Long) {
    InfoPanel(modifier = Modifier.fillMaxWidth()) {
        Text(text = "COMPLETED", style = MaterialTheme.typography.labelSmall)
        Text(
            text = tripDateFormatter.format(Date(endMs)),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private val tripDateFormatter by lazy { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

// UnusedPrivateMember: preview-only composables, see WearRootScreen.kt's identical suppression
// note for the same known detekt false positive on @Preview functions.
@Suppress("DEPRECATION", "UnusedPrivateMember")
@Preview(name = "Wear trip list", device = Devices.WEAR_OS_SMALL_ROUND, showBackground = true)
@Composable
private fun TripListScreenPreview() {
    WearMilewayTheme {
        AppScaffold {
            val listState = rememberScalingLazyListState()
            ScreenScaffold(scrollState = listState) {
                TripListScreen(
                    trips =
                        listOf(
                            TripListItemUi(id = "1", label = "Commute", km = 12.4, endMs = 0L),
                            TripListItemUi(id = "2", label = "Errand", km = 3.2, endMs = 0L),
                        ),
                    listState = listState,
                    onTripClick = {},
                )
            }
        }
    }
}

@Suppress("DEPRECATION", "UnusedPrivateMember")
@Preview(name = "Wear trip detail", device = Devices.WEAR_OS_SMALL_ROUND, showBackground = true)
@Composable
private fun TripDetailScreenPreview() {
    WearMilewayTheme {
        AppScaffold {
            val listState = rememberScalingLazyListState()
            ScreenScaffold(scrollState = listState) {
                TripDetailScreen(
                    trip = TripListItemUi(id = "1", label = "Commute", km = 12.4, endMs = 0L),
                    listState = listState,
                )
            }
        }
    }
}
