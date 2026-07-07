package com.mileway.feature.logging.ui.screens

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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_filter_all
import com.mileway.core.ui.resources.logging_no_settlements_subtitle
import com.mileway.core.ui.resources.logging_no_settlements_title
import com.mileway.core.ui.resources.logging_plural_items
import com.mileway.core.ui.resources.logging_settlement_status_pending
import com.mileway.core.ui.resources.logging_settlement_status_processing
import com.mileway.core.ui.resources.logging_settlement_status_settled
import com.mileway.core.ui.resources.logging_settlements_title
import com.mileway.feature.logging.repository.SettlementRecord
import com.mileway.feature.logging.repository.SettlementStatus
import com.mileway.feature.logging.viewmodel.SETTLEMENT_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.SettlementHistoryAction
import com.mileway.feature.logging.viewmodel.SettlementHistoryViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.logging_settlements_title),
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(SettlementHistoryAction.Refresh) },
        modifier = modifier,
        tabs = SETTLEMENT_HISTORY_TABS.map { it?.localizedLabel() ?: stringResource(Res.string.logging_filter_all) },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(SettlementHistoryAction.SelectTab(it)) },
        emptyTitle = stringResource(Res.string.logging_no_settlements_title),
        emptySubtitle = stringResource(Res.string.logging_no_settlements_subtitle),
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
                StatusChip(label = localizedSettlementStatus(settlement.status), tone = toneFor(settlement.status))
            }
            Text(
                "${settlement.method} · ${pluralStringResource(Res.plurals.logging_plural_items, settlement.itemCount, settlement.itemCount)}",
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

/** Localized display label for a settlement's canonical status string; unknown values pass through. */
@Composable
private fun SettlementStatus.localizedLabel(): String =
    when (this) {
        SettlementStatus.PENDING -> stringResource(Res.string.logging_settlement_status_pending)
        SettlementStatus.PROCESSING -> stringResource(Res.string.logging_settlement_status_processing)
        SettlementStatus.SETTLED -> stringResource(Res.string.logging_settlement_status_settled)
    }

@Composable
private fun localizedSettlementStatus(status: String): String =
    when (status) {
        SettlementStatus.PENDING.label -> stringResource(Res.string.logging_settlement_status_pending)
        SettlementStatus.PROCESSING.label -> stringResource(Res.string.logging_settlement_status_processing)
        SettlementStatus.SETTLED.label -> stringResource(Res.string.logging_settlement_status_settled)
        else -> status
    }
