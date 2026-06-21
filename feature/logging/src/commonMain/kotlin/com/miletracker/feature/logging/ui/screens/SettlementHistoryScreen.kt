package com.miletracker.feature.logging.ui.screens

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
import com.miletracker.core.ui.components.StatusTone
import com.miletracker.core.ui.components.scaffold.HistoryListScaffold
import com.miletracker.feature.logging.repository.SettlementRecord
import com.miletracker.feature.logging.repository.SettlementStatus
import com.miletracker.feature.logging.viewmodel.SETTLEMENT_HISTORY_TABS
import com.miletracker.feature.logging.viewmodel.SettlementHistoryAction
import com.miletracker.feature.logging.viewmodel.SettlementHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** SP.2: settlement history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun SettlementHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettlementHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Settlements",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(SettlementHistoryAction.Refresh) },
        modifier = modifier,
        tabs = SETTLEMENT_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(SettlementHistoryAction.SelectTab(it)) },
        emptyTitle = "No settlements",
        emptySubtitle = "Reimbursement batches will appear here.",
        itemKey = { it.id },
    ) { settlement ->
        SettlementCard(settlement)
    }
}

@Composable
private fun SettlementCard(settlement: SettlementRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${settlement.periodLabel} · ${settlement.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = settlement.status, tone = toneFor(settlement.status))
            }
            Text(
                "${settlement.method} · ${settlement.itemCount} item${if (settlement.itemCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "₹${settlement.amount.toLong()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun toneFor(status: String): StatusTone =
    when (status) {
        SettlementStatus.PENDING.label -> StatusTone.Warning
        SettlementStatus.PROCESSING.label -> StatusTone.Info
        SettlementStatus.SETTLED.label -> StatusTone.Success
        else -> StatusTone.Neutral
    }
