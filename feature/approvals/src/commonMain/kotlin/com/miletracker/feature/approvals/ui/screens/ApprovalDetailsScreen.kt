package com.miletracker.feature.approvals.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.asString
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.ui.mvi.dataOrNull
import com.miletracker.core.ui.theme.MilewayColors
import com.miletracker.feature.approvals.model.ApprovalStatus
import com.miletracker.feature.approvals.model.ApprovalType
import com.miletracker.feature.approvals.ui.sheets.SeekClarificationSheet
import com.miletracker.feature.approvals.viewmodel.ApprovalsAction
import com.miletracker.feature.approvals.viewmodel.ApprovalsEffect
import com.miletracker.feature.approvals.viewmodel.ApprovalsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalDetailsScreen(
    approvalId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApprovalsViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(approvalId) { viewModel.onAction(ApprovalsAction.OpenDetail(approvalId)) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ApprovalsEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message.asString())
                ApprovalsEffect.NavigateBack -> onBack()
                is ApprovalsEffect.NavigateToDetail -> Unit
            }
        }
    }

    val detail = ui.detailState.dataOrNull ?: return
    val item = detail.item

    val effectiveStatus = detail.localStatus ?: item.status
    val isResolved = effectiveStatus != ApprovalStatus.PENDING

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = typeLabel(item.type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "Approval request", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Requester info card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item.requesterName.first().toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.requesterName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Submitted a ${typeLabel(item.type).lowercase()} request",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (item.policyViolation) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Policy violation",
                                    tint = MilewayColors.warning,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                // Context card
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Request Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        DetailRow(label = "Type", value = typeLabel(item.type))
                        DetailRow(label = "Summary", value = item.summary)
                        DetailRow(label = "Amount", value = "₹${item.amountRupees.formatDecimal(2)}")
                        DetailRow(label = "Status", value = effectiveStatus.name)
                    }
                }

                // Policy violation card
                if (item.policyViolation) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MilewayColors.warning.copy(alpha = 0.1f)),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MilewayColors.warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Policy Violation",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MilewayColors.warning,
                                )
                                Text("This request exceeds policy limits", style = MaterialTheme.typography.bodySmall, color = MilewayColors.warning)
                            }
                        }
                    }
                }

                // Resolved status banner
                AnimatedVisibility(visible = isResolved, enter = fadeIn()) {
                    val (icon, color, label) =
                        when (effectiveStatus) {
                            ApprovalStatus.APPROVED -> Triple(Icons.Default.CheckCircle, MilewayColors.success, "You approved this request")
                            ApprovalStatus.REJECTED -> Triple(Icons.Default.Cancel, MilewayColors.danger, "You rejected this request")
                            else -> Triple(Icons.Default.CheckCircle, Color.Gray, "Resolved")
                        }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Pinned CTA bar
            if (!isResolved) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.onAction(ApprovalsAction.OpenClarificationSheet) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clarify")
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Reject) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.danger),
                        ) {
                            Text("Reject")
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Approve) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.success),
                        ) {
                            Text("Approve")
                        }
                    }
                }
            }
        }
    }

    if (detail.showClarificationSheet) {
        SeekClarificationSheet(
            thread = detail.thread,
            draftMessage = detail.draftMessage,
            onDraftChange = { viewModel.onAction(ApprovalsAction.UpdateDraftMessage(it)) },
            onSend = { viewModel.onAction(ApprovalsAction.SendClarification) },
            onDismiss = { viewModel.onAction(ApprovalsAction.CloseClarificationSheet) },
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
    }
}

private fun typeLabel(type: ApprovalType) =
    when (type) {
        ApprovalType.MILEAGE -> "Mileage"
        ApprovalType.EXPENSE -> "Expense"
        ApprovalType.TRAVEL -> "Travel"
        ApprovalType.ADVANCE -> "Advance"
    }
