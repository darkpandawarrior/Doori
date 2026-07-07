package com.mileway.feature.approvals.ui.screens

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
import com.mileway.core.common.asString
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_action_approve
import com.mileway.core.ui.resources.approvals_action_clarify
import com.mileway.core.ui.resources.approvals_action_reject
import com.mileway.core.ui.resources.approvals_cd_back
import com.mileway.core.ui.resources.approvals_field_amount
import com.mileway.core.ui.resources.approvals_field_status
import com.mileway.core.ui.resources.approvals_field_summary
import com.mileway.core.ui.resources.approvals_field_type
import com.mileway.core.ui.resources.approvals_policy_violation
import com.mileway.core.ui.resources.approvals_policy_violation_message
import com.mileway.core.ui.resources.approvals_policy_violation_title
import com.mileway.core.ui.resources.approvals_request_details
import com.mileway.core.ui.resources.approvals_resolved
import com.mileway.core.ui.resources.approvals_submitted_request
import com.mileway.core.ui.resources.approvals_subtitle_approval_request
import com.mileway.core.ui.resources.approvals_type_advance
import com.mileway.core.ui.resources.approvals_type_expense
import com.mileway.core.ui.resources.approvals_type_mileage
import com.mileway.core.ui.resources.approvals_type_travel
import com.mileway.core.ui.resources.approvals_you_approved
import com.mileway.core.ui.resources.approvals_you_rejected
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.ui.sheets.SeekClarificationSheet
import com.mileway.feature.approvals.viewmodel.ApprovalsAction
import com.mileway.feature.approvals.viewmodel.ApprovalsEffect
import com.mileway.feature.approvals.viewmodel.ApprovalsViewModel
import org.jetbrains.compose.resources.stringResource
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
                        Text(
                            text = stringResource(Res.string.approvals_subtitle_approval_request),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.approvals_cd_back))
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
                                    stringResource(Res.string.approvals_submitted_request, typeLabel(item.type).lowercase()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (item.policyViolation) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = stringResource(Res.string.approvals_policy_violation),
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
                        Text(
                            stringResource(Res.string.approvals_request_details),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        DetailRow(label = stringResource(Res.string.approvals_field_type), value = typeLabel(item.type))
                        DetailRow(label = stringResource(Res.string.approvals_field_summary), value = item.summary)
                        DetailRow(label = stringResource(Res.string.approvals_field_amount), value = "₹${item.amountRupees.formatDecimal(2)}")
                        DetailRow(label = stringResource(Res.string.approvals_field_status), value = effectiveStatus.name)
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
                                    stringResource(Res.string.approvals_policy_violation_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MilewayColors.warning,
                                )
                                Text(
                                    stringResource(Res.string.approvals_policy_violation_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MilewayColors.warning,
                                )
                            }
                        }
                    }
                }

                // Resolved status banner
                AnimatedVisibility(visible = isResolved, enter = fadeIn()) {
                    val (icon, color, label) =
                        when (effectiveStatus) {
                            ApprovalStatus.APPROVED ->
                                Triple(
                                    Icons.Default.CheckCircle,
                                    MilewayColors.success,
                                    stringResource(Res.string.approvals_you_approved),
                                )
                            ApprovalStatus.REJECTED -> Triple(Icons.Default.Cancel, MilewayColors.danger, stringResource(Res.string.approvals_you_rejected))
                            else -> Triple(Icons.Default.CheckCircle, Color.Gray, stringResource(Res.string.approvals_resolved))
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
                            Text(stringResource(Res.string.approvals_action_clarify))
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Reject) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.danger),
                        ) {
                            Text(stringResource(Res.string.approvals_action_reject))
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Approve) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.success),
                        ) {
                            Text(stringResource(Res.string.approvals_action_approve))
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

@Composable
private fun typeLabel(type: ApprovalType) =
    when (type) {
        ApprovalType.MILEAGE -> stringResource(Res.string.approvals_type_mileage)
        ApprovalType.EXPENSE -> stringResource(Res.string.approvals_type_expense)
        ApprovalType.TRAVEL -> stringResource(Res.string.approvals_type_travel)
        ApprovalType.ADVANCE -> stringResource(Res.string.approvals_type_advance)
    }
