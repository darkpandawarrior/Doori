package com.mileway.feature.advances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Receipt
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
import com.mileway.core.ui.resources.advances_action_add_expense
import com.mileway.core.ui.resources.advances_action_log_miles
import com.mileway.core.ui.resources.advances_action_recharge_logs
import com.mileway.core.ui.resources.advances_action_track_miles
import com.mileway.core.ui.resources.advances_cd_back
import com.mileway.core.ui.resources.advances_empty_transactions
import com.mileway.core.ui.resources.advances_low_balance_warning
import com.mileway.core.ui.resources.advances_petty_detail_subtitle
import com.mileway.core.ui.resources.advances_petty_detail_title
import com.mileway.core.ui.resources.advances_summary_description
import com.mileway.core.ui.resources.advances_summary_spent
import com.mileway.core.ui.resources.advances_summary_title
import com.mileway.core.ui.resources.advances_summary_type
import com.mileway.core.ui.resources.advances_summary_validity
import com.mileway.core.ui.resources.advances_zero_balance_warning
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.ui.components.BalanceBanner
import com.mileway.feature.advances.ui.components.DetailTabsRow
import com.mileway.feature.advances.ui.components.PettyAdvanceCardFace
import com.mileway.feature.advances.ui.components.QuickAction
import com.mileway.feature.advances.ui.components.QuickActionsRow
import com.mileway.feature.advances.ui.components.SummaryField
import com.mileway.feature.advances.ui.components.SummarySection
import com.mileway.feature.advances.ui.components.TransactionsSection
import com.mileway.feature.advances.ui.components.formatDate
import com.mileway.feature.advances.ui.components.formatMoney
import com.mileway.feature.advances.viewmodel.AdvanceDetailTab
import com.mileway.feature.advances.viewmodel.PettyCardDetailAction
import com.mileway.feature.advances.viewmodel.PettyCardDetailUiState
import com.mileway.feature.advances.viewmodel.PettyCardDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V35.P4: petty-advance detail — card face, low/zero-balance banners, quick actions (hoisted,
 * no cross-feature navigation baked in here), Summary + Transactions tabs.
 */
@Composable
fun PettyCardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onAddExpense: () -> Unit = {},
    onLogMiles: () -> Unit = {},
    onTrackMiles: () -> Unit = {},
    onRechargeLogs: () -> Unit = {},
    viewModel: PettyCardDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(cardId) { viewModel.onAction(PettyCardDetailAction.Load(cardId)) }

    PettyCardDetailContent(state, viewModel::onAction, onBack, onAddExpense, onLogMiles, onTrackMiles, onRechargeLogs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PettyCardDetailContent(
    state: PettyCardDetailUiState,
    onAction: (PettyCardDetailAction) -> Unit,
    onBack: () -> Unit,
    onAddExpense: () -> Unit = {},
    onLogMiles: () -> Unit = {},
    onTrackMiles: () -> Unit = {},
    onRechargeLogs: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.advances_petty_detail_title),
                subtitle = stringResource(Res.string.advances_petty_detail_subtitle),
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
                PettyAdvanceCardFace(card)
                BalanceBannerFor(card)
                QuickActionsRow(
                    actions =
                        listOf(
                            QuickAction(stringResource(Res.string.advances_action_add_expense), Icons.Filled.Receipt, onClick = onAddExpense),
                            QuickAction(stringResource(Res.string.advances_action_log_miles), Icons.Filled.DirectionsCar, onClick = onLogMiles),
                            QuickAction(stringResource(Res.string.advances_action_track_miles), Icons.Filled.MyLocation, onClick = onTrackMiles),
                            QuickAction(stringResource(Res.string.advances_action_recharge_logs), Icons.Filled.History, onClick = onRechargeLogs),
                        ),
                )
                HorizontalDivider()
                DetailTabsRow(selected = state.tab, onSelect = { onAction(PettyCardDetailAction.SelectTab(it)) })
                when (state.tab) {
                    AdvanceDetailTab.SUMMARY -> SummarySection(fields = card.toSummaryFields())
                    AdvanceDetailTab.TRANSACTIONS ->
                        TransactionsSection(
                            query = state.query,
                            onQueryChange = { onAction(PettyCardDetailAction.SetQuery(it)) },
                            filter = state.voucherFilter,
                            onFilterChange = { onAction(PettyCardDetailAction.SetVoucherFilter(it)) },
                            transactions = state.filteredTransactions,
                            emptyText = stringResource(Res.string.advances_empty_transactions),
                        )
                }
            }
        }
    }
}

@Composable
private fun BalanceBannerFor(card: PettyCard) {
    val ratio = if (card.amount > 0.0) card.balance / card.amount else 0.0
    when {
        card.balance <= 0.0 -> BalanceBanner(stringResource(Res.string.advances_zero_balance_warning), StatusTone.Error)
        ratio < 0.2 -> BalanceBanner(stringResource(Res.string.advances_low_balance_warning), StatusTone.Warning)
    }
}

@Composable
private fun PettyCard.toSummaryFields(): List<SummaryField> =
    listOf(
        SummaryField(stringResource(Res.string.advances_summary_title), title),
        SummaryField(stringResource(Res.string.advances_summary_type), type),
        SummaryField(stringResource(Res.string.advances_summary_description), description),
        SummaryField(stringResource(Res.string.advances_summary_spent), "₹${formatMoney(amount - balance)}"),
        SummaryField(stringResource(Res.string.advances_summary_validity), formatDate(dueOnMs)),
    )
