package com.miletracker.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.StatusChip
import com.miletracker.core.ui.components.scaffold.HistoryListScaffold
import com.miletracker.feature.travel.model.TripRecord
import com.miletracker.feature.travel.viewmodel.TRIP_HISTORY_TABS
import com.miletracker.feature.travel.viewmodel.TripHistoryAction
import com.miletracker.feature.travel.viewmodel.TripHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** TR.8 — trip-request history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun TripHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Trip History",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(TripHistoryAction.Refresh) },
        modifier = modifier,
        tabs = TRIP_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(TripHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(TripHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search trips…",
        emptyTitle = "No trips here",
        emptySubtitle = "Submitted trip requests appear under their status.",
        itemKey = { it.id },
    ) { trip ->
        TripCard(trip)
    }
}

@Composable
private fun TripCard(trip: TripRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(trip.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = trip.status.label, tone = travelStatusTone(trip.status))
            }
            Text(
                trip.purpose,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                trip.route,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
