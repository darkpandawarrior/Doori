package com.mileway.feature.advances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.sheet.DetailInfoRow
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_filter_all
import com.mileway.core.ui.resources.advances_filter_has_voucher
import com.mileway.core.ui.resources.advances_filter_no_voucher
import com.mileway.core.ui.resources.advances_search_transactions
import com.mileway.core.ui.resources.advances_tab_summary
import com.mileway.core.ui.resources.advances_tab_transactions
import com.mileway.core.ui.resources.advances_voucher_created
import com.mileway.core.ui.resources.advances_voucher_missing
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.viewmodel.AdvanceDetailTab
import com.mileway.feature.advances.viewmodel.VoucherFilter
import org.jetbrains.compose.resources.stringResource

/*
 * PLAN_V35.P4: pieces shared by PettyCardDetailScreen and QrCardDetailScreen — low/zero-balance
 * banners, the quick-actions row, the Summary field list, and the Transactions search+filter+list.
 */

@Composable
internal fun BalanceBanner(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tone.color.copy(alpha = 0.15f),
        shape = DesignTokens.Shape.roundedSm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(
                imageVector = if (tone == StatusTone.Error) Icons.Filled.ErrorOutline else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = tone.color,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = tone.color)
        }
    }
}

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickActionsRow(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        actions.forEach { action ->
            OutlinedButton(onClick = action.onClick, enabled = action.enabled, shape = DesignTokens.Shape.button) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(action.label)
            }
        }
    }
}

data class SummaryField(val label: String, val value: String)

@Composable
internal fun SummarySection(
    fields: List<SummaryField>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        fields.forEach { field -> DetailInfoRow(field.label, field.value) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionsSection(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: VoucherFilter,
    onFilterChange: (VoucherFilter) -> Unit,
    transactions: List<AdvanceTransaction>,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(Res.string.advances_search_transactions)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            FilterChip(
                selected = filter == VoucherFilter.ALL,
                onClick = { onFilterChange(VoucherFilter.ALL) },
                label = { Text(stringResource(Res.string.advances_filter_all)) },
            )
            FilterChip(
                selected = filter == VoucherFilter.NO_VOUCHER,
                onClick = { onFilterChange(VoucherFilter.NO_VOUCHER) },
                label = { Text(stringResource(Res.string.advances_filter_no_voucher)) },
            )
            FilterChip(
                selected = filter == VoucherFilter.HAS_VOUCHER,
                onClick = { onFilterChange(VoucherFilter.HAS_VOUCHER) },
                label = { Text(stringResource(Res.string.advances_filter_has_voucher)) },
            )
        }
        if (transactions.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                transactions.forEach { txn -> AdvanceTransactionRow(txn) }
            }
        }
    }
}

/** Summary / Transactions tab row, shared by both detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailTabsRow(
    selected: AdvanceDetailTab,
    onSelect: (AdvanceDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp, modifier = modifier) {
        Tab(
            selected = selected == AdvanceDetailTab.SUMMARY,
            onClick = { onSelect(AdvanceDetailTab.SUMMARY) },
            text = { Text(stringResource(Res.string.advances_tab_summary)) },
        )
        Tab(
            selected = selected == AdvanceDetailTab.TRANSACTIONS,
            onClick = { onSelect(AdvanceDetailTab.TRANSACTIONS) },
            text = { Text(stringResource(Res.string.advances_tab_transactions)) },
        )
    }
}

@Composable
internal fun AdvanceTransactionRow(
    txn: AdvanceTransaction,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(txn.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(formatDate(txn.dateMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${formatMoney(txn.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(4.dp))
            StatusChip(
                label =
                    stringResource(
                        if (txn.voucherCreated) Res.string.advances_voucher_created else Res.string.advances_voucher_missing,
                    ),
                tone = if (txn.voucherCreated) StatusTone.Success else StatusTone.Neutral,
            )
        }
    }
}
