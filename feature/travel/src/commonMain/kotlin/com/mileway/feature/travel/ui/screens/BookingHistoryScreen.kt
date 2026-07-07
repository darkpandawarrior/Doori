package com.mileway.feature.travel.ui.screens

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
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_booking_empty_subtitle
import com.mileway.core.ui.resources.travel_booking_empty_title
import com.mileway.core.ui.resources.travel_booking_history_title
import com.mileway.core.ui.resources.travel_booking_search_placeholder
import com.mileway.core.ui.resources.travel_filter_all
import com.mileway.feature.travel.model.BookingRequest
import com.mileway.feature.travel.model.TravelReqStatus
import com.mileway.feature.travel.viewmodel.BOOKING_HISTORY_TABS
import com.mileway.feature.travel.viewmodel.BookingHistoryAction
import com.mileway.feature.travel.viewmodel.BookingHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** TR.8: unified booking-request history (Flight / Bus / Hotel / MJP / Visa) on the shared HistoryListScaffold. */
@Composable
fun BookingHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    val allLabel = stringResource(Res.string.travel_filter_all)
    HistoryListScaffold(
        title = stringResource(Res.string.travel_booking_history_title),
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(BookingHistoryAction.Refresh) },
        modifier = modifier,
        tabs = BOOKING_HISTORY_TABS.map { it?.localizedLabel() ?: allLabel },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(BookingHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(BookingHistoryAction.SetQuery(it)) },
        searchPlaceholder = stringResource(Res.string.travel_booking_search_placeholder),
        emptyTitle = stringResource(Res.string.travel_booking_empty_title),
        emptySubtitle = stringResource(Res.string.travel_booking_empty_subtitle),
        filterChips = {
            StatusChipFilter(allLabel, ui.statusFilter == null) {
                viewModel.onAction(BookingHistoryAction.SetStatusFilter(null))
            }
            TravelReqStatus.entries.forEach { status ->
                StatusChipFilter(status.localizedLabel(), ui.statusFilter == status) {
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
                Text("${booking.type.localizedLabel()} · ${booking.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = booking.status.localizedLabel(), tone = travelStatusTone(booking.status))
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
