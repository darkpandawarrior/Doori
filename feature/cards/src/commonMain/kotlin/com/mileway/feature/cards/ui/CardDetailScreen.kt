package com.mileway.feature.cards.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.components.sheet.DetailInfoBottomSheet
import com.mileway.core.ui.components.sheet.DetailInfoCard
import com.mileway.core.ui.components.sheet.DetailInfoRow
import com.mileway.core.ui.components.timeline.TimelineStep
import com.mileway.core.ui.components.timeline.TransactionTimeline
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.cards_add_address_details
import com.mileway.core.ui.resources.cards_address
import com.mileway.core.ui.resources.cards_address_details_message
import com.mileway.core.ui.resources.cards_amount
import com.mileway.core.ui.resources.cards_approval_workflow
import com.mileway.core.ui.resources.cards_available_balance
import com.mileway.core.ui.resources.cards_back
import com.mileway.core.ui.resources.cards_block
import com.mileway.core.ui.resources.cards_category
import com.mileway.core.ui.resources.cards_city
import com.mileway.core.ui.resources.cards_claim_expense
import com.mileway.core.ui.resources.cards_daily_limit_label
import com.mileway.core.ui.resources.cards_daily_limit_message
import com.mileway.core.ui.resources.cards_detail_subtitle
import com.mileway.core.ui.resources.cards_detail_title
import com.mileway.core.ui.resources.cards_dispute
import com.mileway.core.ui.resources.cards_dispute_message
import com.mileway.core.ui.resources.cards_dispute_submit
import com.mileway.core.ui.resources.cards_dispute_transaction
import com.mileway.core.ui.resources.cards_freeze
import com.mileway.core.ui.resources.cards_history_empty
import com.mileway.core.ui.resources.cards_issue_physical_card
import com.mileway.core.ui.resources.cards_kyc_pending_message
import com.mileway.core.ui.resources.cards_locality
import com.mileway.core.ui.resources.cards_monthly_limit_label
import com.mileway.core.ui.resources.cards_monthly_limit_message
import com.mileway.core.ui.resources.cards_pincode
import com.mileway.core.ui.resources.cards_send_kyc_link
import com.mileway.core.ui.resources.cards_set_daily_limit
import com.mileway.core.ui.resources.cards_set_limit
import com.mileway.core.ui.resources.cards_set_monthly_limit
import com.mileway.core.ui.resources.cards_set_single_limit
import com.mileway.core.ui.resources.cards_ship_card
import com.mileway.core.ui.resources.cards_single_limit_label
import com.mileway.core.ui.resources.cards_single_limit_message
import com.mileway.core.ui.resources.cards_state
import com.mileway.core.ui.resources.cards_status
import com.mileway.core.ui.resources.cards_status_kyc_pending
import com.mileway.core.ui.resources.cards_tab_history
import com.mileway.core.ui.resources.cards_tab_transactions
import com.mileway.core.ui.resources.cards_transaction
import com.mileway.core.ui.resources.cards_transaction_no
import com.mileway.core.ui.resources.cards_unblock
import com.mileway.core.ui.resources.cards_unfreeze
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.dataStyle
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.cards.model.ApprovalStepModel
import com.mileway.feature.cards.model.ApprovalStepStatus
import com.mileway.feature.cards.model.CARD_DISPUTE_REASONS
import com.mileway.feature.cards.model.CardAuditEntry
import com.mileway.feature.cards.model.CardModel
import com.mileway.feature.cards.model.CardShippingAddress
import com.mileway.feature.cards.model.CardStatus
import com.mileway.feature.cards.model.CardTransactionModel
import com.mileway.feature.cards.model.CardTxnClaimStatus
import com.mileway.feature.cards.model.LimitKind
import com.mileway.feature.cards.ui.components.CardAccent
import com.mileway.feature.cards.ui.components.CardFace
import com.mileway.feature.cards.ui.components.formatMoney
import com.mileway.feature.cards.viewmodel.CardDetailAction
import com.mileway.feature.cards.viewmodel.CardDetailEffect
import com.mileway.feature.cards.viewmodel.CardDetailTab
import com.mileway.feature.cards.viewmodel.CardDetailUiState
import com.mileway.feature.cards.viewmodel.CardDetailViewModel
import com.siddharth.kmp.common.asString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    // P27.E.7: default no-op keeps every existing call site (including the screenshot gallery)
    // unchanged; the app shell's cardsGraph supplies the real navigation.
    onClaimTransaction: (ExpenseSourceContext) -> Unit = {},
    // P29.C.1: default no-op preserves old call sites; CardsNavigation supplies real navigation
    // to the KYC wizard. Replaces the old toast-only "resend" stub.
    onStartKyc: () -> Unit = {},
    // P29.C.1: set true for exactly one recomposition after CardKycScreen reports completion
    // (via CardsNavigation's savedStateHandle result) — flips isKycPending, then the caller
    // acknowledges via [onKycAcknowledged] so it doesn't re-fire.
    kycJustVerified: Boolean = false,
    onKycAcknowledged: () -> Unit = {},
    viewModel: CardDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cardId) { viewModel.onAction(CardDetailAction.Load(cardId)) }
    LaunchedEffect(kycJustVerified) {
        if (kycJustVerified) {
            viewModel.onAction(CardDetailAction.KycVerified)
            onKycAcknowledged()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CardDetailEffect.ShowToast ->
                    Toasts.show(title = effect.message.asString(), description = "", type = ToastType.Success)
                is CardDetailEffect.NavigateToExpenseEntry -> onClaimTransaction(effect.context)
            }
        }
    }

    CardDetailContent(state, viewModel::onAction, onBack, onStartKyc)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardDetailContent(
    state: CardDetailUiState,
    onAction: (CardDetailAction) -> Unit,
    onBack: () -> Unit,
    onStartKyc: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.cards_detail_title),
                subtitle = stringResource(Res.string.cards_detail_subtitle),
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cards_back))
                    }
                },
            )
        },
    ) { padding ->
        ScreenStateContent(
            state = state.card,
            modifier = Modifier.padding(padding),
        ) { card ->
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CardFace(card)
                BalanceHeader(card)
                if (card.isKycPending) KycPendingBanner(onResend = onStartKyc)
                CardControls(card, onAction)
                if (state.approvalSteps.isNotEmpty()) {
                    TransactionTimeline(
                        steps = state.approvalSteps.map { it.toTimelineStep() },
                        title = stringResource(Res.string.cards_approval_workflow),
                    )
                }
                HorizontalDivider()
                DetailTabs(state.detailTab, onSelect = { onAction(CardDetailAction.SelectDetailTab(it)) })
                when (state.detailTab) {
                    CardDetailTab.TRANSACTIONS ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClaimTabs(state.claimTab, onSelect = { onAction(CardDetailAction.SelectClaimTab(it)) })
                            ScreenStateContent(state = state.transactions, modifier = Modifier.fillMaxWidth()) { txns ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    txns.forEach { txn ->
                                        TransactionRow(txn, onClick = { onAction(CardDetailAction.OpenTransaction(txn)) })
                                    }
                                }
                            }
                        }
                    CardDetailTab.HISTORY -> AuditHistorySection(state.auditLog)
                }
            }
        }
    }

    if (state.showMonthlyLimitDialog) {
        LimitSheet(
            title = stringResource(Res.string.cards_set_monthly_limit),
            message = stringResource(Res.string.cards_monthly_limit_message),
            label = stringResource(Res.string.cards_monthly_limit_label),
            onConfirm = { onAction(CardDetailAction.SetMonthlyLimit(it)) },
            onDismiss = { onAction(CardDetailAction.DismissMonthlyLimit) },
        )
    }
    state.limitSheetKind?.let { kind ->
        val (title, message, label) =
            when (kind) {
                LimitKind.SINGLE_TRANSACTION ->
                    Triple(
                        stringResource(Res.string.cards_set_single_limit),
                        stringResource(Res.string.cards_single_limit_message),
                        stringResource(Res.string.cards_single_limit_label),
                    )
                LimitKind.DAILY ->
                    Triple(
                        stringResource(Res.string.cards_set_daily_limit),
                        stringResource(Res.string.cards_daily_limit_message),
                        stringResource(Res.string.cards_daily_limit_label),
                    )
            }
        LimitSheet(
            title = title,
            message = message,
            label = label,
            onConfirm = { onAction(CardDetailAction.SetLimit(kind, it)) },
            onDismiss = { onAction(CardDetailAction.DismissLimitSheet) },
        )
    }
    if (state.showPhysicalCardDialog) {
        PhysicalCardSheet(
            onConfirm = { onAction(CardDetailAction.IssuePhysicalCard(it)) },
            onDismiss = { onAction(CardDetailAction.DismissPhysicalCard) },
        )
    }
    state.selectedTransaction?.let { txn ->
        TransactionDetailSheet(
            txn = txn,
            onClaim = { onAction(CardDetailAction.ClaimTransaction(txn.id)) },
            onDispute = { onAction(CardDetailAction.OpenDispute(txn.id)) },
            onDismiss = { onAction(CardDetailAction.DismissTransaction) },
        )
    }
    if (state.disputingTransactionId != null) {
        DisputeReasonSheet(
            onConfirm = { onAction(CardDetailAction.SubmitDispute(it)) },
            onDismiss = { onAction(CardDetailAction.DismissDispute) },
        )
    }
}

@Composable
private fun ApprovalStepModel.toTimelineStep(): TimelineStep =
    TimelineStep(
        label = title,
        icon = if (status == ApprovalStepStatus.APPROVED) Icons.Filled.CheckCircle else Icons.Filled.Person,
        color = if (status == ApprovalStepStatus.APPROVED) DesignTokens.StatusColors.success else MaterialTheme.colorScheme.primary,
        active = status == ApprovalStepStatus.APPROVED,
        note = approverName ?: "",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTabs(
    selected: CardDetailTab,
    onSelect: (CardDetailTab) -> Unit,
) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp) {
        Tab(
            selected = selected == CardDetailTab.TRANSACTIONS,
            onClick = { onSelect(CardDetailTab.TRANSACTIONS) },
            text = { Text(stringResource(Res.string.cards_tab_transactions)) },
        )
        Tab(
            selected = selected == CardDetailTab.HISTORY,
            onClick = { onSelect(CardDetailTab.HISTORY) },
            text = { Text(stringResource(Res.string.cards_tab_history)) },
        )
    }
}

/** P29.C.5: chronological render of the session-scoped audit log (freeze/block/limit/KYC actions). */
@Composable
private fun AuditHistorySection(entries: List<CardAuditEntry>) {
    if (entries.isEmpty()) {
        Text(
            stringResource(Res.string.cards_history_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    TransactionTimeline(
        steps =
            entries.reversed().map {
                TimelineStep(label = it.action, icon = Icons.Filled.Tune, color = MaterialTheme.colorScheme.primary, active = true, note = it.detail)
            },
    )
}

@Composable
private fun BalanceHeader(card: CardModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(card.cardHolderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            card.employeeEmail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(Res.string.cards_available_balance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(formatMoney(card.balance, card.currency), style = MaterialTheme.typography.titleLarge.dataStyle(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KycPendingBanner(onResend: () -> Unit) {
    Card(
        shape = DesignTokens.Shape.roundedSm,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.cards_status_kyc_pending), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.cards_kyc_pending_message),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onResend, shape = DesignTokens.Shape.button) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_send_kyc_link))
            }
        }
    }
}

@Composable
private fun CardControls(
    card: CardModel,
    onAction: (CardDetailAction) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onAction(CardDetailAction.ToggleBlock) },
            modifier = Modifier.weight(1f),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(
                if (card.status == CardStatus.BLOCKED) Icons.Filled.CheckCircle else Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(8.dp))
            Text(if (card.status == CardStatus.BLOCKED) stringResource(Res.string.cards_unblock) else stringResource(Res.string.cards_block))
        }
        OutlinedButton(
            onClick = { onAction(CardDetailAction.ToggleFreeze) },
            modifier = Modifier.weight(1f),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(
                if (card.isFrozen) Icons.Filled.LockOpen else Icons.Filled.AcUnit,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(8.dp))
            Text(if (card.isFrozen) stringResource(Res.string.cards_unfreeze) else stringResource(Res.string.cards_freeze))
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onAction(CardDetailAction.OpenMonthlyLimit) },
            modifier = Modifier.weight(1f),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.cards_set_monthly_limit))
        }
        if (card.status != CardStatus.PHYSICAL_ISSUED) {
            OutlinedButton(
                onClick = { onAction(CardDetailAction.OpenPhysicalCard) },
                modifier = Modifier.weight(1f),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_issue_physical_card))
            }
        }
    }
    // P29.C.3: single-txn/daily limits, parameterized by LimitKind (separate from the monthly-limit
    // control above, which mutates a different field).
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onAction(CardDetailAction.OpenLimitSheet(LimitKind.SINGLE_TRANSACTION)) },
            modifier = Modifier.weight(1f),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.cards_set_single_limit))
        }
        OutlinedButton(
            onClick = { onAction(CardDetailAction.OpenLimitSheet(LimitKind.DAILY)) },
            modifier = Modifier.weight(1f),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.cards_set_daily_limit))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimTabs(
    selected: CardTxnClaimStatus,
    onSelect: (CardTxnClaimStatus) -> Unit,
) {
    val entries = CardTxnClaimStatus.entries
    ScrollableTabRow(selectedTabIndex = entries.indexOf(selected), edgePadding = 0.dp) {
        entries.forEach { status ->
            Tab(
                selected = status == selected,
                onClick = { onSelect(status) },
                text = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                icon = { Icon(claimStatusIcon(status), contentDescription = null) },
            )
        }
    }
}

private fun claimStatusIcon(status: CardTxnClaimStatus) =
    when (status) {
        CardTxnClaimStatus.UNCLAIMED -> Icons.Filled.Receipt
        CardTxnClaimStatus.PERSONAL -> Icons.Filled.Person
        CardTxnClaimStatus.CLAIMED -> Icons.Filled.CheckCircle
        CardTxnClaimStatus.RECOVERED -> Icons.AutoMirrored.Filled.Undo
        CardTxnClaimStatus.REJECTED -> Icons.Filled.Cancel
    }

@Composable
private fun TransactionRow(
    txn: CardTransactionModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(txn.merchantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(txn.txnNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatMoney(txn.amount, txn.currency), style = MaterialTheme.typography.bodyMedium.dataStyle(), color = CardAccent)
    }
}

/** P29.C.3: generalized from the old `MonthlyLimitSheet` — same shape, parameterized copy so it also
 * drives the single-transaction/daily limit sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitSheet(
    title: String,
    message: String,
    label: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { ch -> ch.isDigit() } },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { value.toDoubleOrNull()?.let(onConfirm) },
                enabled = value.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_set_limit))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhysicalCardSheet(
    onConfirm: (CardShippingAddress) -> Unit,
    onDismiss: () -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    val valid = address.isNotBlank() && city.isNotBlank() && pincode.isNotBlank()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.cards_add_address_details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.cards_address_details_message), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                address,
                { address = it },
                label = { Text(stringResource(Res.string.cards_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(locality, {
                locality = it
            }, label = { Text(stringResource(Res.string.cards_locality)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                city,
                { city = it },
                label = { Text(stringResource(Res.string.cards_city)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                state,
                { state = it },
                label = { Text(stringResource(Res.string.cards_state)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                pincode,
                { pincode = it },
                label = { Text(stringResource(Res.string.cards_pincode)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onConfirm(CardShippingAddress(address, locality, city, state, pincode)) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_ship_card))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    txn: CardTransactionModel,
    onClaim: () -> Unit,
    onDispute: () -> Unit,
    onDismiss: () -> Unit,
) {
    // SHEETS.D: shared gradient-header + multi-card detail sheet.
    DetailInfoBottomSheet(
        title = txn.merchantName,
        subtitle = formatMoney(txn.amount, txn.currency),
        headerGradient = listOf(Color(0xFF3730A3), Color(0xFF5C6BC0)),
        headerIcon = Icons.Filled.ReceiptLong,
        onDismiss = onDismiss,
    ) {
        DetailInfoCard(title = stringResource(Res.string.cards_transaction)) {
            DetailInfoRow(stringResource(Res.string.cards_transaction_no), txn.txnNumber)
            DetailInfoRow(stringResource(Res.string.cards_category), txn.category)
            DetailInfoRow(stringResource(Res.string.cards_amount), formatMoney(txn.amount, txn.currency))
            DetailInfoRow(stringResource(Res.string.cards_status), txn.claimStatus.name.lowercase().replaceFirstChar { it.uppercase() })
            txn.disputeReason?.let { DetailInfoRow(stringResource(Res.string.cards_dispute), it) }
        }
        if (txn.claimStatus == CardTxnClaimStatus.UNCLAIMED) {
            Button(onClick = onClaim, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_claim_expense))
            }
            OutlinedButton(
                onClick = onDispute,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_dispute))
            }
        }
    }
}

/** P29.C.2: reason picker that flips a transaction to the REJECTED claim tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisputeReasonSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(Res.string.cards_dispute_transaction), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.cards_dispute_message), style = MaterialTheme.typography.bodySmall)
            CARD_DISPUTE_REASONS.forEach { reason ->
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(selected = reason == selected, onClick = { selected = reason }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == selected, onClick = { selected = reason })
                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.cards_dispute_submit))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
