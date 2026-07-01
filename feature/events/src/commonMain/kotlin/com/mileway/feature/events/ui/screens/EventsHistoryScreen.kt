package com.mileway.feature.events.ui.screens

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
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.feature.events.model.EventRecord
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.viewmodel.EVENTS_HISTORY_TABS
import com.mileway.feature.events.viewmodel.EventsHistoryAction
import com.mileway.feature.events.viewmodel.EventsHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** EV: events history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun EventsHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Events",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(EventsHistoryAction.Refresh) },
        modifier = modifier,
        tabs = EVENTS_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(EventsHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(EventsHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search events…",
        emptyTitle = "No events here",
        emptySubtitle = "Created events appear under their status.",
        itemKey = { it.id },
    ) { event ->
        EventCard(event)
    }
}

@Composable
private fun EventCard(event: EventRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = event.status.label, tone = toneFor(event.status))
            }
            Text(
                "${event.venue} · ${event.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "${event.attendees} attendees",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun toneFor(status: EventStatus): StatusTone =
    when (status) {
        EventStatus.DRAFT -> StatusTone.Neutral
        EventStatus.PUBLISHED -> StatusTone.Success
        EventStatus.CANCELLED -> StatusTone.Error
        EventStatus.COMPLETED -> StatusTone.Info
    }
