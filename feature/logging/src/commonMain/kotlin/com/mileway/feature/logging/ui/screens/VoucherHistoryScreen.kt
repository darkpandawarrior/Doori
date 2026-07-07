package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_cancel
import com.mileway.core.ui.resources.logging_no_vouchers_subtitle
import com.mileway.core.ui.resources.logging_no_vouchers_title
import com.mileway.core.ui.resources.logging_search_vouchers_placeholder
import com.mileway.core.ui.resources.logging_voucher_actions_cd
import com.mileway.core.ui.resources.logging_voucher_history_title
import com.mileway.core.ui.resources.logging_withdraw
import com.mileway.core.ui.resources.logging_withdraw_description
import com.mileway.core.ui.resources.logging_withdraw_title
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import com.mileway.feature.logging.viewmodel.VOUCHER_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.VoucherHistoryAction
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.logging_voucher_history_title),
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(VoucherHistoryAction.Refresh) },
        modifier = modifier,
        tabs = VOUCHER_HISTORY_TABS.map { it?.label ?: "All" },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(VoucherHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(VoucherHistoryAction.SetQuery(it)) },
        searchPlaceholder = stringResource(Res.string.logging_search_vouchers_placeholder),
        emptyTitle = stringResource(Res.string.logging_no_vouchers_title),
        emptySubtitle = stringResource(Res.string.logging_no_vouchers_subtitle),
        itemKey = { it.id },
    ) { voucher ->
        VoucherCard(
            voucher = voucher,
            onWithdraw = { viewModel.onAction(VoucherHistoryAction.Withdraw(voucher.id)) },
        )
    }
}

/**
 * P3.6: withdraw is gated to [VoucherStatus.DRAFT] rows only — a deliberate simplification of
 * the reference app's two-factor `draft OR pending-approval` + config-flag gate, since Mileway
 * has no separate "pending approval permission" concept to gate on. Non-DRAFT vouchers don't
 * even show the overflow menu, since there is nothing they can do with it.
 */
@Composable
private fun VoucherCard(
    voucher: SubmittedVoucher,
    onWithdraw: () -> Unit,
) {
    val canWithdraw = voucher.voucherState == VoucherStatus.DRAFT.label
    var menuExpanded by remember { mutableStateOf(false) }
    var showWithdrawConfirmation by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(voucher.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row {
                    StatusChip(label = voucher.voucherState, tone = toneFor(voucher.voucherState))
                    if (canWithdraw) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.logging_voucher_actions_cd))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.logging_withdraw)) },
                                    onClick = {
                                        menuExpanded = false
                                        showWithdrawConfirmation = true
                                    },
                                )
                            }
                        }
                    }
                }
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

    if (showWithdrawConfirmation) {
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.logging_withdraw_title),
            description = stringResource(Res.string.logging_withdraw_description, voucher.id),
            confirmLabel = stringResource(Res.string.logging_withdraw),
            dismissLabel = stringResource(Res.string.logging_cancel),
            icon = Icons.Filled.Delete,
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                showWithdrawConfirmation = false
                onWithdraw()
            },
            onDismiss = { showWithdrawConfirmation = false },
        )
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
