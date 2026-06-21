package com.miletracker.feature.payments.ui.screens

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
import com.miletracker.feature.payments.model.PaymentRecord
import com.miletracker.feature.payments.model.PaymentStatus
import com.miletracker.feature.payments.viewmodel.PAYMENTS_HISTORY_TABS
import com.miletracker.feature.payments.viewmodel.PaymentsHistoryAction
import com.miletracker.feature.payments.viewmodel.PaymentsHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** PM — payments history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun PaymentsHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Payments",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(PaymentsHistoryAction.Refresh) },
        modifier = modifier,
        tabs = PAYMENTS_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(PaymentsHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(PaymentsHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search payments…",
        emptyTitle = "No payments here",
        emptySubtitle = "Your UPI / QR pays and requests appear under their status.",
        itemKey = { it.id },
    ) { payment ->
        PaymentCard(payment)
    }
}

@Composable
private fun PaymentCard(payment: PaymentRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${payment.direction.label} · ${payment.counterparty}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = payment.status.label, tone = toneFor(payment.status))
            }
            if (payment.note.isNotBlank()) {
                Text(
                    payment.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "₹${payment.amount.toLong()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun toneFor(status: PaymentStatus): StatusTone =
    when (status) {
        PaymentStatus.PENDING -> StatusTone.Warning
        PaymentStatus.COMPLETED -> StatusTone.Success
        PaymentStatus.FAILED -> StatusTone.Error
    }
