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
import com.mileway.feature.logging.repository.VoucherStatus
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import com.mileway.feature.logging.viewmodel.VOUCHER_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.VoucherHistoryAction
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/** SP.1: voucher history, built entirely on the shared F0.4 [HistoryListScaffold] + F0.3 [StatusChip]. */
@Composable
fun VoucherHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoucherHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = "Voucher History",
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(VoucherHistoryAction.Refresh) },
        modifier = modifier,
        tabs = VOUCHER_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(VoucherHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(VoucherHistoryAction.SetQuery(it)) },
        searchPlaceholder = "Search vouchers…",
        emptyTitle = "No vouchers here",
        emptySubtitle = "Submitted vouchers will appear under their status.",
        itemKey = { it.id },
    ) { voucher ->
        VoucherCard(voucher)
    }
}

@Composable
private fun VoucherCard(voucher: SubmittedVoucher) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(voucher.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = voucher.voucherState, tone = toneFor(voucher.voucherState))
            }
            Text(
                "${voucher.serviceTag} · ${voucher.office}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "₹${voucher.amount.toLong()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun toneFor(state: String): StatusTone =
    when (state) {
        VoucherStatus.DRAFT.label -> StatusTone.Neutral
        VoucherStatus.PENDING.label -> StatusTone.Warning
        VoucherStatus.APPROVED.label -> StatusTone.Success
        VoucherStatus.REJECTED.label -> StatusTone.Error
        VoucherStatus.SETTLED.label -> StatusTone.Info
        else -> StatusTone.Neutral
    }
