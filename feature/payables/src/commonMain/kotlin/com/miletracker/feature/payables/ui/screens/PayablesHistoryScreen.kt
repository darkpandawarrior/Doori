package com.miletracker.feature.payables.ui.screens

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
import com.miletracker.core.ui.components.StatusTone
import com.miletracker.core.ui.components.scaffold.HistoryListScaffold
import com.miletracker.feature.payables.model.PayablesDoc
import com.miletracker.feature.payables.model.PayablesDocStatus
import com.miletracker.feature.payables.viewmodel.PAYABLES_HISTORY_TABS
import com.miletracker.feature.payables.viewmodel.PayablesHistoryAction
import com.miletracker.feature.payables.viewmodel.PayablesHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * PB.4 — unified payables history (Invoice / PR / GIN / Park In-Out / ASN). Doc-type tabs + a status filter
 * chip row + search, all on the shared F0.4 [HistoryListScaffold] with F0.3 [StatusChip] tones.
 */
@Composable
fun PayablesHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Payables History",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(PayablesHistoryAction.Refresh) },
        modifier = modifier,
        tabs = PAYABLES_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(PayablesHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(PayablesHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search payables…",
        emptyTitle = "No documents here",
        emptySubtitle = "Created payables documents appear under their type and status.",
        filterChips = {
            StatusFilterChip("All", ui.statusFilter == null) {
                viewModel.onAction(PayablesHistoryAction.SetStatusFilter(null))
            }
            PayablesDocStatus.entries.forEach { status ->
                StatusFilterChip(status.label, ui.statusFilter == status) {
                    viewModel.onAction(PayablesHistoryAction.SetStatusFilter(status))
                }
            }
        },
        itemKey = { it.id },
    ) { doc ->
        PayablesDocCard(doc)
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PayablesDocCard(doc: PayablesDoc) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${doc.type.label} · ${doc.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = doc.status.label, tone = toneFor(doc.status))
            }
            Text(
                "${doc.title} · ${doc.reference}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (doc.amount != null) {
                Text(
                    "₹${doc.amount.toLong()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun toneFor(status: PayablesDocStatus): StatusTone =
    when (status) {
        PayablesDocStatus.DRAFT -> StatusTone.Neutral
        PayablesDocStatus.PENDING -> StatusTone.Warning
        PayablesDocStatus.APPROVED -> StatusTone.Success
        PayablesDocStatus.REJECTED -> StatusTone.Error
        PayablesDocStatus.COMPLETED -> StatusTone.Info
    }
