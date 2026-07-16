package com.mileway.feature.advances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_action_recharge_logs
import com.mileway.core.ui.resources.advances_action_scan_qr
import com.mileway.core.ui.resources.advances_action_transfer_money
import com.mileway.core.ui.resources.advances_cd_back
import com.mileway.core.ui.resources.advances_empty_transactions
import com.mileway.core.ui.resources.advances_low_balance_warning
import com.mileway.core.ui.resources.advances_qr_detail_subtitle
import com.mileway.core.ui.resources.advances_qr_detail_title
import com.mileway.core.ui.resources.advances_summary_description
import com.mileway.core.ui.resources.advances_summary_spent
import com.mileway.core.ui.resources.advances_summary_title
import com.mileway.core.ui.resources.advances_summary_validity
import com.mileway.core.ui.resources.advances_zero_balance_warning
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.ui.components.BalanceBanner
import com.mileway.feature.advances.ui.components.DetailTabsRow
import com.mileway.feature.advances.ui.components.QrCardFace
import com.mileway.feature.advances.ui.components.QuickAction
import com.mileway.feature.advances.ui.components.QuickActionsRow
import com.mileway.feature.advances.ui.components.SummaryField
import com.mileway.feature.advances.ui.components.SummarySection
import com.mileway.feature.advances.ui.components.TransactionsSection
import com.mileway.feature.advances.ui.components.formatDate
import com.mileway.feature.advances.ui.components.formatMoney
import com.mileway.feature.advances.viewmodel.AdvanceDetailTab
import com.mileway.feature.advances.viewmodel.QrCardDetailAction
import com.mileway.feature.advances.viewmodel.QrCardDetailUiState
import com.mileway.feature.advances.viewmodel.QrCardDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V35.P4: QR-card detail — card face, low/zero-balance banners, quick actions (hoisted, no
 * cross-feature navigation baked in here — Scan QR is disabled at zero/negative balance), Summary
 * + Transactions (recharge log) tabs.
 */
@Composable
fun QrCardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onScanQr: () -> Unit = {},
    onRechargeLogs: () -> Unit = {},
    onTransferMoney: () -> Unit = {},
    viewModel: QrCardDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(cardId) { viewModel.onAction(QrCardDetailAction.Load(cardId)) }

    QrCardDetailContent(state, viewModel::onAction, onBack, onScanQr, onRechargeLogs, onTransferMoney)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrCardDetailContent(
    state: QrCardDetailUiState,
    onAction: (QrCardDetailAction) -> Unit,
    onBack: () -> Unit,
    onScanQr: () -> Unit = {},
    onRechargeLogs: () -> Unit = {},
    onTransferMoney: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.advances_qr_detail_title),
                subtitle = stringResource(Res.string.advances_qr_detail_subtitle),
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.advances_cd_back))
                    }
                },
            )
        },
    ) { padding ->
        ScreenStateContent(state = state.card, modifier = Modifier.padding(padding)) { card ->
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QrCardFace(card, onScan = onScanQr)
                BalanceBannerFor(card)
                QuickActionsRow(actions = card.quickActions(onScanQr, onRechargeLogs, onTransferMoney))
                HorizontalDivider()
                DetailTabsRow(selected = state.tab, onSelect = { onAction(QrCardDetailAction.SelectTab(it)) })
                when (state.tab) {
                    AdvanceDetailTab.SUMMARY -> SummarySection(fields = card.toSummaryFields())
                    AdvanceDetailTab.TRANSACTIONS ->
                        TransactionsSection(
                            query = state.query,
                            onQueryChange = { onAction(QrCardDetailAction.SetQuery(it)) },
                            filter = state.voucherFilter,
                            onFilterChange = { onAction(QrCardDetailAction.SetVoucherFilter(it)) },
                            transactions = state.filteredTransactions,
                            emptyText = stringResource(Res.string.advances_empty_transactions),
                        )
                }
            }
        }
    }
}

@Composable
private fun QrCard.quickActions(
    onScanQr: () -> Unit,
    onRechargeLogs: () -> Unit,
    onTransferMoney: () -> Unit,
): List<QuickAction> =
    buildList {
        add(QuickAction(stringResource(Res.string.advances_action_scan_qr), Icons.Filled.QrCodeScanner, enabled = balance > 0.0, onClick = onScanQr))
        add(QuickAction(stringResource(Res.string.advances_action_recharge_logs), Icons.Filled.History, onClick = onRechargeLogs))
        if (isTransfer) {
            add(QuickAction(stringResource(Res.string.advances_action_transfer_money), Icons.AutoMirrored.Filled.Send, onClick = onTransferMoney))
        }
    }

@Composable
private fun BalanceBannerFor(card: QrCard) {
    val ratio = if (card.total > 0.0) card.balance / card.total else 0.0
    when {
        card.balance <= 0.0 -> BalanceBanner(stringResource(Res.string.advances_zero_balance_warning), StatusTone.Error)
        ratio < 0.2 -> BalanceBanner(stringResource(Res.string.advances_low_balance_warning), StatusTone.Warning)
    }
}

@Composable
private fun QrCard.toSummaryFields(): List<SummaryField> =
    listOf(
        SummaryField(stringResource(Res.string.advances_summary_title), title),
        SummaryField(stringResource(Res.string.advances_summary_description), description),
        SummaryField(stringResource(Res.string.advances_summary_spent), "₹${formatMoney(total - balance)}"),
        SummaryField(stringResource(Res.string.advances_summary_validity), formatDate(validUntilMs)),
    )
