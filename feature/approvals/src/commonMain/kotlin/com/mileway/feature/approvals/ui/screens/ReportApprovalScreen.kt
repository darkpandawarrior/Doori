package com.mileway.feature.approvals.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.ui.components.scaffold.DetailSection
import com.mileway.core.ui.components.scaffold.TransactionDetailScaffold
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.model.toDetailActionFlags
import com.mileway.feature.approvals.ui.sheets.SeekClarificationSheet
import com.mileway.feature.approvals.viewmodel.ReportApprovalViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Persisted report review, reached from the approvals queue and lifecycle inbox links. */
@Composable
fun ReportApprovalScreen(
    reportId: String,
    onBack: () -> Unit,
    viewModel: ReportApprovalViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    var showRoom by remember(reportId) { mutableStateOf(false) }
    LaunchedEffect(reportId) { viewModel.open(reportId) }
    TransactionDetailScaffold(
        title = "Report $reportId",
        subtitle = "Local review · Simulated payout",
        tabs = listOf(DetailSection.Details),
        selectedTab = DetailSection.Details,
        onSelectTab = {},
        onBack = onBack,
    ) {
        ReportApprovalContent(
            ui = ui,
            onComment = viewModel::comment,
            onAct = viewModel::act,
            onRetry = viewModel::retryPayout,
            onClarify = {
                viewModel.openClarification()
                showRoom = true
            },
        )
    }
    if (showRoom) {
        SeekClarificationSheet(
            room = ui.room,
            thread = ui.thread,
            draftMessage = ui.draftMessage,
            onDraftChange = viewModel::draftMessage,
            onSend = viewModel::sendMessage,
            onRequestCloseRoom = viewModel::closeRoom,
            onDismiss = { showRoom = false },
            draftAttachmentUrl = ui.draftAttachmentUrl,
            onDraftAttachmentChange = viewModel::draftAttachment,
        )
    }
}

@Composable
private fun ReportApprovalContent(
    ui: ReportApprovalViewModel.State,
    onComment: (String) -> Unit,
    onAct: (ApprovalAction) -> Unit,
    onRetry: () -> Unit,
    onClarify: () -> Unit,
) {
    val report = ui.report
    var acknowledged by remember(report?.id, report?.recordVersion) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (report == null) {
            Text(if (ui.loading) "Loading report…" else "Report not found")
            return@Column
        }
        Text("Employee: ${report.employeeId}")
        Text("Status: ${report.state.name.replace('_', ' ')}")
        report.lines.forEach { line -> Text("${line.id}: ${formatReportAmount(line.amountMinor, line.currency)}") }
        Text("Total: ${formatReportAmount(report.totalAmountMinor(), report.currency())}")
        report.approvalChain.steps.forEach { step ->
            Text("${step.action}: ${step.actedBy} (${step.role}) · ${step.comment.orEmpty()}")
        }
        OutlinedButton(onClick = onClarify, enabled = !ui.busy) { Text("Clarification room") }
        if (report.state == ReportLifecycleState.SUBMITTED) {
            val item =
                ApprovalItem(
                    id = report.id,
                    type = ApprovalType.EXPENSE,
                    requesterName = report.employeeId,
                    summary = "Report",
                    amountRupees = 0.0,
                    status = ApprovalStatus.PENDING,
                    timestampMs = 0,
                    policyViolation = report.lines.any { it.policyFlags.isNotEmpty() },
                )
            val flags = item.toDetailActionFlags()
            if (flags.requiresAck) {
                Row {
                    Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
                    Text("I reviewed the policy flags")
                }
            }
            OutlinedTextField(
                value = ui.comment,
                onValueChange = onComment,
                label = { Text("Approval comment (required)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.busy,
            )
            val canAct = !ui.busy && ui.comment.isNotBlank()
            Button(onClick = { onAct(ApprovalAction.APPROVE) }, enabled = canAct && (!flags.requiresAck || acknowledged)) {
                Text("Approve and simulate payout")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onAct(ApprovalAction.SEND_BACK) }, enabled = canAct) { Text("Send back") }
                OutlinedButton(onClick = { onAct(ApprovalAction.REJECT) }, enabled = canAct) { Text("Reject") }
            }
        }
        if (report.state == ReportLifecycleState.APPROVED || report.state == ReportLifecycleState.APPROVED_FOR_PAYMENT) {
            Button(onClick = onRetry, enabled = !ui.busy) { Text("Retry simulated payout") }
        }
    }
}

@Preview
@Composable
private fun ReportApprovalPreview() {
    MaterialTheme {
        ReportApprovalContent(
            ui =
                ReportApprovalViewModel.State(
                    report =
                        Report(
                            "report-1",
                            "employee",
                            listOf(
                                ExpenseLine(
                                    "receipt-1",
                                    5000,
                                    "INR",
                                    merchant = "Cafe",
                                    category = "Meals",
                                ),
                            ),
                            state = ReportLifecycleState.SUBMITTED,
                        ),
                    loading = false,
                ),
            onComment = {},
            onAct = {},
            onRetry = {},
            onClarify = {},
        )
    }
}

private const val ReportFractionDigits = 2

// ponytail: current report currencies have two fraction digits; add currency fraction metadata
// when zero- or three-digit currencies enter the claim flow. String math preserves every cent.
internal fun formatReportAmount(
    minor: Long,
    currency: String,
): String {
    val digits = minor.toString().removePrefix("-").padStart(ReportFractionDigits + 1, '0')
    val sign = if (minor < 0) "-" else ""
    return "$currency $sign${digits.dropLast(ReportFractionDigits)}.${digits.takeLast(ReportFractionDigits)}"
}
