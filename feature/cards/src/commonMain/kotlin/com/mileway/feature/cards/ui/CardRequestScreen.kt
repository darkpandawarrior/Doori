package com.mileway.feature.cards.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.common.asString
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.cards_agree_policies
import com.mileway.core.ui.resources.cards_ai_suggested
import com.mileway.core.ui.resources.cards_back
import com.mileway.core.ui.resources.cards_confirm_card_type
import com.mileway.core.ui.resources.cards_confirm_reason
import com.mileway.core.ui.resources.cards_confirm_scheme
import com.mileway.core.ui.resources.cards_done
import com.mileway.core.ui.resources.cards_next
import com.mileway.core.ui.resources.cards_request_a_card
import com.mileway.core.ui.resources.cards_request_in_workflow
import com.mileway.core.ui.resources.cards_request_intro
import com.mileway.core.ui.resources.cards_request_reason_label
import com.mileway.core.ui.resources.cards_request_submitted
import com.mileway.core.ui.resources.cards_request_subtitle
import com.mileway.core.ui.resources.cards_submit
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.cards.viewmodel.CardRequestAction
import com.mileway.feature.cards.viewmodel.CardRequestEffect
import com.mileway.feature.cards.viewmodel.CardRequestUiState
import com.mileway.feature.cards.viewmodel.CardRequestViewModel
import org.jetbrains.compose.resources.stringResource
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
                title = stringResource(Res.string.cards_request_a_card),
                subtitle = stringResource(Res.string.cards_request_subtitle),
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cards_back))
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
                            stringResource(Res.string.cards_request_intro),
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
                                            Text(
                                                "  • " + stringResource(Res.string.cards_ai_suggested),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
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
                            label = { Text(stringResource(Res.string.cards_request_reason_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                    else -> ConfirmStep(state, onAction)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = { onAction(CardRequestAction.Back) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(Res.string.cards_back)) }
                }
                if (state.step < state.totalSteps - 1) {
                    Button(onClick = {
                        onAction(CardRequestAction.Next)
                    }, enabled = state.canAdvance, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.cards_next)) }
                } else {
                    Button(onClick = {
                        onAction(CardRequestAction.Submit)
                    }, enabled = state.agreeToPolicies, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.cards_submit)) }
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
            Text(stringResource(Res.string.cards_confirm_card_type, type?.name ?: "-"), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.cards_confirm_scheme, type?.scheme ?: "-"), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(Res.string.cards_confirm_reason, state.reason), style = MaterialTheme.typography.bodySmall)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = state.agreeToPolicies, onCheckedChange = { onAction(CardRequestAction.SetAgree(it)) })
        Text(stringResource(Res.string.cards_agree_policies), style = MaterialTheme.typography.bodySmall)
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
        Text(stringResource(Res.string.cards_request_submitted), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.cards_request_in_workflow, requestId),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) { Text(stringResource(Res.string.cards_done)) }
    }
}
