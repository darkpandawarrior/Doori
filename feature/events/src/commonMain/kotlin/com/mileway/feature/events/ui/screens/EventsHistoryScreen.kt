package com.mileway.feature.events.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.events_attendees_count
import com.mileway.core.ui.resources.events_empty_subtitle
import com.mileway.core.ui.resources.events_empty_title
import com.mileway.core.ui.resources.events_history_subtitle
import com.mileway.core.ui.resources.events_history_title
import com.mileway.core.ui.resources.events_search_placeholder
import com.mileway.core.ui.resources.events_status_cancelled
import com.mileway.core.ui.resources.events_status_completed
import com.mileway.core.ui.resources.events_status_draft
import com.mileway.core.ui.resources.events_status_pending_approval
import com.mileway.core.ui.resources.events_status_published
import com.mileway.core.ui.resources.events_tab_all
import com.mileway.feature.events.model.EventRecord
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.viewmodel.EVENTS_HISTORY_TABS
import com.mileway.feature.events.viewmodel.EventsHistoryAction
import com.mileway.feature.events.viewmodel.EventsHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** EV: events history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun EventsHistoryScreen(
    onBack: () -> Unit,
    onOpenEvent: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EventsHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    val allLabel = stringResource(Res.string.events_tab_all)
    HistoryListScaffold(
        title = stringResource(Res.string.events_history_title),
        subtitle = stringResource(Res.string.events_history_subtitle),
        titleIcon = Icons.Filled.Event,
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(EventsHistoryAction.Refresh) },
        modifier = modifier,
        tabs = EVENTS_HISTORY_TABS.map { it?.localizedLabel() ?: allLabel },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(EventsHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(EventsHistoryAction.SetQuery(it)) },
        searchPlaceholder = stringResource(Res.string.events_search_placeholder),
        emptyTitle = stringResource(Res.string.events_empty_title),
        emptySubtitle = stringResource(Res.string.events_empty_subtitle),
        itemKey = { it.id },
    ) { event ->
        EventCard(event, onClick = { onOpenEvent(event.id) })
    }
}

@Composable
private fun EventCard(
    event: EventRecord,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = event.status.localizedLabel(), tone = toneFor(event.status))
            }
            Text(
                "${event.venue} · ${event.category.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(Res.string.events_attendees_count, event.attendees),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

internal fun toneFor(status: EventStatus): StatusTone =
    when (status) {
        EventStatus.DRAFT -> StatusTone.Neutral
        EventStatus.PENDING_APPROVAL -> StatusTone.Warning
        EventStatus.PUBLISHED -> StatusTone.Success
        EventStatus.CANCELLED -> StatusTone.Error
        EventStatus.COMPLETED -> StatusTone.Info
    }

/** Localized display label for an event status; the enum's `label` stays canonical for search. */
@Composable
internal fun EventStatus.localizedLabel(): String =
    when (this) {
        EventStatus.DRAFT -> stringResource(Res.string.events_status_draft)
        EventStatus.PENDING_APPROVAL -> stringResource(Res.string.events_status_pending_approval)
        EventStatus.PUBLISHED -> stringResource(Res.string.events_status_published)
        EventStatus.CANCELLED -> stringResource(Res.string.events_status_cancelled)
        EventStatus.COMPLETED -> stringResource(Res.string.events_status_completed)
    }
