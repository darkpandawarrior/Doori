package com.miletracker.feature.cards.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.miletracker.core.common.asString
import com.miletracker.core.ui.components.sheet.DetailInfoBottomSheet
import com.miletracker.core.ui.components.sheet.DetailInfoCard
import com.miletracker.core.ui.components.sheet.DetailInfoRow
import com.miletracker.core.ui.mvi.ScreenStateContent
import com.miletracker.core.ui.theme.dataStyle
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.cards.model.CardModel
import com.miletracker.feature.cards.model.CardShippingAddress
import com.miletracker.feature.cards.model.CardStatus
import com.miletracker.feature.cards.model.CardTransactionModel
import com.miletracker.feature.cards.model.CardTxnClaimStatus
import com.miletracker.feature.cards.ui.components.CardAccent
import com.miletracker.feature.cards.ui.components.CardFace
import com.miletracker.feature.cards.ui.components.formatMoney
import com.miletracker.feature.cards.viewmodel.CardDetailAction
import com.miletracker.feature.cards.viewmodel.CardDetailEffect
import com.miletracker.feature.cards.viewmodel.CardDetailUiState
import com.miletracker.feature.cards.viewmodel.CardDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    viewModel: CardDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cardId) { viewModel.onAction(CardDetailAction.Load(cardId)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CardDetailEffect.ShowToast ->
                    Toasts.show(title = effect.message.asString(), description = "", type = ToastType.Success)
            }
        }
    }

    CardDetailContent(state, viewModel::onAction, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardDetailContent(
    state: CardDetailUiState,
    onAction: (CardDetailAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                if (card.isKycPending) KycPendingBanner(onResend = { onAction(CardDetailAction.ResendKyc) })
                CardControls(card, onAction)
                HorizontalDivider()
                Text("Transactions", style = MaterialTheme.typography.titleMedium)
                ClaimTabs(state.claimTab, onSelect = { onAction(CardDetailAction.SelectClaimTab(it)) })
                ScreenStateContent(state = state.transactions, modifier = Modifier.fillMaxWidth()) { txns ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        txns.forEach { txn ->
                            TransactionRow(txn, onClick = { onAction(CardDetailAction.OpenTransaction(txn)) })
                        }
                    }
                }
            }
        }
    }

    if (state.showMonthlyLimitDialog) {
        MonthlyLimitSheet(
            onConfirm = { onAction(CardDetailAction.SetMonthlyLimit(it)) },
            onDismiss = { onAction(CardDetailAction.DismissMonthlyLimit) },
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
            onDismiss = { onAction(CardDetailAction.DismissTransaction) },
        )
    }
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
            Text("Available Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(card.balance, card.currency), style = MaterialTheme.typography.titleLarge.dataStyle(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KycPendingBanner(onResend: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("KYC Pending", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "KYC is pending for your account. We've emailed you a KYC link; tap below to resend it.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onResend) { Text("Send KYC Link") }
        }
    }
}

@Composable
private fun CardControls(
    card: CardModel,
    onAction: (CardDetailAction) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onAction(CardDetailAction.ToggleBlock) }, modifier = Modifier.weight(1f)) {
            Text(if (card.status == CardStatus.BLOCKED) "Unblock" else "Block")
        }
        OutlinedButton(onClick = { onAction(CardDetailAction.ToggleFreeze) }, modifier = Modifier.weight(1f)) {
            Text(if (card.isFrozen) "Unfreeze" else "Freeze")
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onAction(CardDetailAction.OpenMonthlyLimit) }, modifier = Modifier.weight(1f)) {
            Text("Set Monthly Limit")
        }
        if (card.status != CardStatus.PHYSICAL_ISSUED) {
            OutlinedButton(onClick = { onAction(CardDetailAction.OpenPhysicalCard) }, modifier = Modifier.weight(1f)) {
                Text("Issue Physical Card")
            }
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
            )
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyLimitSheet(
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
            Text("Set Monthly Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Enter the monthly limit for this card.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monthly Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { value.toDoubleOrNull()?.let(onConfirm) },
                enabled = value.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Set Limit") }
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
            Text("Add Address Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Enter address details to ship your physical card.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(address, { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(locality, { locality = it }, label = { Text("Locality") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(city, { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state, { state = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pincode, { pincode = it }, label = { Text("Pincode") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onConfirm(CardShippingAddress(address, locality, city, state, pincode)) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ship Card") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    txn: CardTransactionModel,
    onClaim: () -> Unit,
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
        DetailInfoCard(title = "Transaction") {
            DetailInfoRow("Transaction no.", txn.txnNumber)
            DetailInfoRow("Category", txn.category)
            DetailInfoRow("Amount", formatMoney(txn.amount, txn.currency))
            DetailInfoRow("Status", txn.claimStatus.name.lowercase().replaceFirstChar { it.uppercase() })
        }
        if (txn.claimStatus == CardTxnClaimStatus.UNCLAIMED) {
            Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                Text("Claim expense")
            }
        }
    }
}
