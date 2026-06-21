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
import com.miletracker.feature.logging.repository.CardExpenseTxn
import com.miletracker.feature.logging.repository.CardTxnStatus
import com.miletracker.feature.logging.viewmodel.CARDS_TXN_HISTORY_TABS
import com.miletracker.feature.logging.viewmodel.CardsTxnHistoryAction
import com.miletracker.feature.logging.viewmodel.CardsTxnHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** SP.3: cards-expense-txn history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun CardsTxnHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardsTxnHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Card Transactions",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(CardsTxnHistoryAction.Refresh) },
        modifier = modifier,
        tabs = CARDS_TXN_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(CardsTxnHistoryAction.SelectTab(it)) },
        emptyTitle = "No transactions",
        emptySubtitle = "Corporate-card spend awaiting reconciliation appears here.",
        itemKey = { it.id },
    ) { txn ->
        CardTxnCard(txn)
    }
}

@Composable
private fun CardTxnCard(txn: CardExpenseTxn) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(txn.merchant, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = txn.status, tone = toneFor(txn.status))
            }
            Text(
                "${txn.category} · •••• ${txn.cardLast4}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "₹${txn.amount.toLong()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun toneFor(status: String): StatusTone =
    when (status) {
        CardTxnStatus.UNRECONCILED.label -> StatusTone.Warning
        CardTxnStatus.RECONCILED.label -> StatusTone.Success
        CardTxnStatus.DISPUTED.label -> StatusTone.Error
        else -> StatusTone.Neutral
    }
