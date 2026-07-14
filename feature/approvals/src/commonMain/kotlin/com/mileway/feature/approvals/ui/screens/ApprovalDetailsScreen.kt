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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siddharth.kmp.common.formatDecimal
import com.mileway.core.ui.components.scaffold.DetailSection
import com.mileway.core.ui.components.scaffold.TransactionDetailScaffold
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.timeline.TimelineStep
import com.mileway.core.ui.components.timeline.TransactionTimeline
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_ack_violation
import com.mileway.core.ui.resources.approvals_action_approve
import com.mileway.core.ui.resources.approvals_action_cancel
import com.mileway.core.ui.resources.approvals_action_clarify
import com.mileway.core.ui.resources.approvals_action_edit_distance
import com.mileway.core.ui.resources.approvals_action_reject
import com.mileway.core.ui.resources.approvals_action_withdraw
import com.mileway.core.ui.resources.approvals_close_room_confirm
import com.mileway.core.ui.resources.approvals_close_room_description
import com.mileway.core.ui.resources.approvals_close_room_title
import com.mileway.core.ui.resources.approvals_field_amount
import com.mileway.core.ui.resources.approvals_field_status
import com.mileway.core.ui.resources.approvals_field_summary
import com.mileway.core.ui.resources.approvals_field_type
import com.mileway.core.ui.resources.approvals_policy_violation
import com.mileway.core.ui.resources.approvals_policy_violation_message
import com.mileway.core.ui.resources.approvals_policy_violation_title
import com.mileway.core.ui.resources.approvals_request_details
import com.mileway.core.ui.resources.approvals_resolved
import com.mileway.core.ui.resources.approvals_status_approved
import com.mileway.core.ui.resources.approvals_status_pending
import com.mileway.core.ui.resources.approvals_status_rejected
import com.mileway.core.ui.resources.approvals_submitted_request
import com.mileway.core.ui.resources.approvals_subtitle_approval_request
import com.mileway.core.ui.resources.approvals_type_advance
import com.mileway.core.ui.resources.approvals_type_expense
import com.mileway.core.ui.resources.approvals_type_mileage
import com.mileway.core.ui.resources.approvals_type_travel
import com.mileway.core.ui.resources.approvals_you_approved
import com.mileway.core.ui.resources.approvals_you_rejected
import com.mileway.core.ui.resources.audit_flag_flagged
import com.mileway.core.ui.resources.audit_flag_merchant_verified
import com.mileway.core.ui.resources.audit_flag_receipt_verified
import com.mileway.core.ui.resources.audit_flag_rejected_reason
import com.mileway.core.ui.resources.audit_flag_violations
import com.mileway.core.ui.resources.detail_section_comments_empty
import com.mileway.core.ui.resources.detail_section_comments_placeholder
import com.mileway.core.ui.resources.detail_section_comments_post
import com.mileway.core.ui.resources.shared_status_submitted
import com.mileway.core.ui.resources.shared_status_under_review
import com.mileway.core.ui.text.getText
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.approvals.model.ApprovalComment
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.model.AuditFlags
import com.mileway.feature.approvals.model.toAuditFlags
import com.mileway.feature.approvals.model.toDetailActionFlags
import com.mileway.feature.approvals.ui.sheets.SeekClarificationSheet
import com.mileway.feature.approvals.viewmodel.ApprovalsAction
import com.mileway.feature.approvals.viewmodel.ApprovalsEffect
import com.mileway.feature.approvals.viewmodel.ApprovalsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
                is ApprovalsEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message.getText())
                ApprovalsEffect.NavigateBack -> onBack()
                is ApprovalsEffect.NavigateToDetail -> Unit
            }
        }
    }

    val detail = ui.detailState.dataOrNull ?: return
    val item = detail.item

    val effectiveStatus = detail.localStatus ?: item.status
    val isResolved = effectiveStatus != ApprovalStatus.PENDING
    val actionFlags = remember(item, effectiveStatus) { item.toDetailActionFlags(effectiveStatus) }
    val auditFlags = remember(item) { item.toAuditFlags() }

    // ponytail: which tab is showing is pure UI navigation state, not ViewModel-worthy (see the
    // same note on ExpenseDetailScreen / PurchaseRequestDetailsScreen).
    var selectedSection by remember { mutableStateOf<DetailSection>(DetailSection.Details) }

    Box(modifier = modifier.fillMaxSize()) {
        TransactionDetailScaffold(
            title = typeLabel(item.type),
            subtitle = stringResource(Res.string.approvals_subtitle_approval_request),
            tabs = listOf(DetailSection.Details, DetailSection.Timeline, DetailSection.Comments, DetailSection.Audit),
            selectedTab = selectedSection,
            onSelectTab = { selectedSection = it },
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        ) { section ->
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
                when (section) {
                    DetailSection.Timeline -> TransactionTimeline(steps = buildApprovalTimelineSteps(item, effectiveStatus))
                    DetailSection.Comments ->
                        CommentsSection(
                            comments = detail.comments,
                            draft = detail.commentDraft,
                            onDraftChange = { viewModel.onAction(ApprovalsAction.UpdateCommentDraft(it)) },
                            onPost = { viewModel.onAction(ApprovalsAction.PostComment) },
                        )
                    DetailSection.Audit -> AuditSection(auditFlags)
                    else -> {
                        // Requester info card
                        Card(
                            shape = DesignTokens.Shape.button,
                            elevation = CardDefaults.cardElevation(2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier =
                                            Modifier.size(40.dp).clip(DesignTokens.Shape.button)
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
                        Card(shape = DesignTokens.Shape.button, elevation = CardDefaults.cardElevation(2.dp)) {
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
                                shape = DesignTokens.Shape.button,
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
                                    ApprovalStatus.REJECTED ->
                                        Triple(Icons.Default.Cancel, MilewayColors.danger, stringResource(Res.string.approvals_you_rejected))
                                    else -> Triple(Icons.Default.CheckCircle, Color.Gray, stringResource(Res.string.approvals_resolved))
                                }
                            Card(
                                shape = DesignTokens.Shape.button,
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
                }
            }
        }

        // Pinned CTA bar — floats over the scaffold (topbar + tabs + content) regardless of
        // which tab is selected, since Approve/Reject/Clarify act on the approval as a whole.
        // P28.9: "own" requests (canWithdraw) get Withdraw/Edit-Distance instead of the
        // approver's Approve/Reject; a flagged request (requiresAck) gates Approve/Reject behind
        // the acknowledgement checkbox.
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
                if (actionFlags.requiresAck) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = detail.acknowledged,
                            onCheckedChange = { viewModel.onAction(ApprovalsAction.ToggleAcknowledged) },
                        )
                        Text(stringResource(Res.string.approvals_ack_violation), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (actionFlags.canWithdraw) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (actionFlags.canEditDistance) {
                            OutlinedButton(
                                onClick = { viewModel.onAction(ApprovalsAction.RequestEditDistance) },
                                modifier = Modifier.weight(1f),
                                shape = DesignTokens.Shape.button,
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.approvals_action_edit_distance))
                            }
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Withdraw) },
                            modifier = Modifier.weight(1f),
                            shape = DesignTokens.Shape.button,
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.danger),
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.approvals_action_withdraw))
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.onAction(ApprovalsAction.OpenClarificationSheet) },
                            modifier = Modifier.weight(1f),
                            shape = DesignTokens.Shape.button,
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.approvals_action_clarify))
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Reject) },
                            enabled = !actionFlags.requiresAck || detail.acknowledged,
                            modifier = Modifier.weight(1f),
                            shape = DesignTokens.Shape.button,
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.danger),
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.approvals_action_reject))
                        }
                        Button(
                            onClick = { viewModel.onAction(ApprovalsAction.Approve) },
                            enabled = !actionFlags.requiresAck || detail.acknowledged,
                            modifier = Modifier.weight(1f),
                            shape = DesignTokens.Shape.button,
                            colors = ButtonDefaults.buttonColors(containerColor = MilewayColors.success),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.approvals_action_approve))
                        }
                    }
                }
            }
        }
    }

    if (detail.showClarificationSheet) {
        SeekClarificationSheet(
            room = detail.room,
            thread = detail.thread,
            meta = detail.roomMeta,
            draftMessage = detail.draftMessage,
            onDraftChange = { viewModel.onAction(ApprovalsAction.UpdateDraftMessage(it)) },
            onSend = { viewModel.onAction(ApprovalsAction.SendClarification) },
            onRequestCloseRoom = { viewModel.onAction(ApprovalsAction.RequestCloseRoom) },
            onToggleSaved = { viewModel.onAction(ApprovalsAction.ToggleRoomSaved) },
            onTogglePinned = { viewModel.onAction(ApprovalsAction.ToggleRoomPinned) },
            onDismiss = { viewModel.onAction(ApprovalsAction.CloseClarificationSheet) },
            draftAttachmentUrl = detail.draftAttachmentUrl,
            onDraftAttachmentChange = { viewModel.onAction(ApprovalsAction.UpdateDraftAttachment(it)) },
        )
    }

    if (detail.showCloseRoomConfirmation) {
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.approvals_close_room_title),
            description = stringResource(Res.string.approvals_close_room_description),
            confirmLabel = stringResource(Res.string.approvals_close_room_confirm),
            dismissLabel = stringResource(Res.string.approvals_action_cancel),
            icon = Icons.Filled.Lock,
            tone = ActionConfirmationToneType.Warning,
            onConfirm = { viewModel.onAction(ApprovalsAction.ConfirmCloseRoom) },
            onDismiss = { viewModel.onAction(ApprovalsAction.DismissCloseRoomConfirmation) },
        )
    }
}

@Composable
private fun buildApprovalTimelineSteps(
    item: ApprovalItem,
    effectiveStatus: ApprovalStatus,
): List<TimelineStep> {
    val submitted =
        TimelineStep(
            label = stringResource(Res.string.shared_status_submitted),
            icon = Icons.Filled.Receipt,
            color = StatusColors.info,
            active = true,
            note = formatSubmittedDate(item.timestampMs),
        )
    val underReview =
        TimelineStep(
            label = stringResource(Res.string.shared_status_under_review),
            icon = Icons.Filled.HourglassBottom,
            color = StatusColors.warning,
            active = true,
        )
    val terminal =
        when (effectiveStatus) {
            ApprovalStatus.APPROVED ->
                TimelineStep(
                    label = stringResource(Res.string.approvals_status_approved),
                    icon = Icons.Filled.CheckCircle,
                    color = StatusColors.success,
                    active = true,
                )
            ApprovalStatus.REJECTED ->
                TimelineStep(
                    label = stringResource(Res.string.approvals_status_rejected),
                    icon = Icons.Filled.Cancel,
                    color = StatusColors.error,
                    active = true,
                )
            ApprovalStatus.PENDING ->
                TimelineStep(
                    label = stringResource(Res.string.approvals_status_pending),
                    icon = Icons.Filled.HourglassBottom,
                    color = StatusColors.neutral,
                    active = false,
                )
        }
    return listOf(submitted, underReview, terminal)
}

/** P28.7: the permanent comments tab — a plain list + composer, no lifecycle unlike the clarification chat. */
@Composable
private fun CommentsSection(
    comments: List<ApprovalComment>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onPost: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (comments.isEmpty()) {
            Text(
                stringResource(Res.string.detail_section_comments_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            comments.forEach { comment ->
                Card(shape = DesignTokens.Shape.button, elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${comment.authorName} · ${comment.designation}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(comment.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.detail_section_comments_placeholder)) },
            )
            IconButton(onClick = onPost, enabled = draft.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = stringResource(Res.string.detail_section_comments_post))
            }
        }
    }
}

/** P28.8: renders the mock/derived [AuditFlags] as a simple field list. */
@Composable
private fun AuditSection(flags: AuditFlags) {
    Card(shape = DesignTokens.Shape.button, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(label = stringResource(Res.string.audit_flag_receipt_verified), value = flags.receiptVerified.toString())
            DetailRow(label = stringResource(Res.string.audit_flag_merchant_verified), value = flags.merchantVerified.toString())
            DetailRow(label = stringResource(Res.string.audit_flag_flagged), value = flags.flagged.toString())
            if (flags.violations.isNotEmpty()) {
                DetailRow(label = stringResource(Res.string.audit_flag_violations), value = flags.violations.joinToString())
            }
            flags.rejectedReason?.let { DetailRow(label = stringResource(Res.string.audit_flag_rejected_reason), value = it) }
        }
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

private val TIMELINE_MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatSubmittedDate(ms: Long): String {
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.dayOfMonth} ${TIMELINE_MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
}
