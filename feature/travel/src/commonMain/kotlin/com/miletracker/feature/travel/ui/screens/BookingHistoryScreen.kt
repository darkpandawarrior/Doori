package com.miletracker.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.miletracker.feature.travel.model.BookingRequest
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.viewmodel.BOOKING_HISTORY_TABS
import com.miletracker.feature.travel.viewmodel.BookingHistoryAction
import com.miletracker.feature.travel.viewmodel.BookingHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** TR.8 — unified booking-request history (Flight / Bus / Hotel / MJP / Visa) on the shared HistoryListScaffold. */
@Composable
fun BookingHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Booking History",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(BookingHistoryAction.Refresh) },
        modifier = modifier,
        tabs = BOOKING_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(BookingHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(BookingHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search bookings…",
        emptyTitle = "No bookings here",
        emptySubtitle = "Submitted booking requests appear under their type and status.",
        filterChips = {
            StatusChipFilter("All", ui.statusFilter == null) {
                viewModel.onAction(BookingHistoryAction.SetStatusFilter(null))
            }
            TravelReqStatus.entries.forEach { status ->
                StatusChipFilter(status.label, ui.statusFilter == status) {
                    viewModel.onAction(BookingHistoryAction.SetStatusFilter(status))
                }
            }
        },
        itemKey = { it.id },
    ) { booking ->
        BookingCard(booking)
    }
}

@Composable
private fun StatusChipFilter(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun BookingCard(booking: BookingRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${booking.type.label} · ${booking.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = booking.status.label, tone = travelStatusTone(booking.status))
            }
            Text(
                booking.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (booking.amount != null) {
                Text(
                    "₹${booking.amount.toLong()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
