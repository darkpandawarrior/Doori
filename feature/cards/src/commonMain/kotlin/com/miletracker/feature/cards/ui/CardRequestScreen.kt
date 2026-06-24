package com.miletracker.feature.cards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.common.asString
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.cards.viewmodel.CardRequestAction
import com.miletracker.feature.cards.viewmodel.CardRequestEffect
import com.miletracker.feature.cards.viewmodel.CardRequestUiState
import com.miletracker.feature.cards.viewmodel.CardRequestViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardRequestScreen(
    onDone: () -> Unit,
    viewModel: CardRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CardRequestEffect.ShowToast ->
                    Toasts.show(title = effect.message.asString(), description = "", type = ToastType.Success)
            }
        }
    }
    CardRequestContent(state, viewModel::onAction, onDone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardRequestContent(
    state: CardRequestUiState,
    onAction: (CardRequestAction) -> Unit,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Request a card",
                subtitle = "KYC-lite application form",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.submittedRequestId != null) {
            SuccessContent(state.submittedRequestId, onDone, Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (state.step + 1f) / state.totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.step) {
                    0 ->
                        Text(
                            "Request a corporate card. You'll pick a card type, give a reason, and confirm. " +
                                "Approvals route through your manager and finance.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    1 ->
                        state.cardTypes.forEach { type ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth().selectable(
                                        selected = state.selectedCardTypeId == type.id,
                                        onClick = { onAction(CardRequestAction.SelectType(type.id)) },
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = state.selectedCardTypeId == type.id, onClick = { onAction(CardRequestAction.SelectType(type.id)) })
                                Column {
                                    Row {
                                        Text(type.name, fontWeight = FontWeight.Medium)
                                        if (type.isAiSuggested) {
                                            Text("  • AI suggested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    2 ->
                        OutlinedTextField(
                            value = state.reason,
                            onValueChange = { onAction(CardRequestAction.SetReason(it)) },
                            label = { Text("Why do you need this card?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                    else -> ConfirmStep(state, onAction)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.step > 0) {
                    OutlinedButton(onClick = { onAction(CardRequestAction.Back) }, modifier = Modifier.weight(1f)) { Text("Back") }
                }
                if (state.step < state.totalSteps - 1) {
                    Button(onClick = { onAction(CardRequestAction.Next) }, enabled = state.canAdvance, modifier = Modifier.weight(1f)) { Text("Next") }
                } else {
                    Button(onClick = { onAction(CardRequestAction.Submit) }, enabled = state.agreeToPolicies, modifier = Modifier.weight(1f)) { Text("Submit") }
                }
            }
        }
    }
}

@Composable
private fun ConfirmStep(
    state: CardRequestUiState,
    onAction: (CardRequestAction) -> Unit,
) {
    val type = state.cardTypes.firstOrNull { it.id == state.selectedCardTypeId }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Card type: ${type?.name ?: "-"}", style = MaterialTheme.typography.bodyMedium)
            Text("Scheme: ${type?.scheme ?: "-"}", style = MaterialTheme.typography.bodySmall)
            Text("Reason: ${state.reason}", style = MaterialTheme.typography.bodySmall)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = state.agreeToPolicies, onCheckedChange = { onAction(CardRequestAction.SetAgree(it)) })
        Text("I agree to the corporate card policies.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SuccessContent(
    requestId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Request submitted", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Your request (#$requestId) is now in the approval workflow.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) { Text("Done") }
    }
}
