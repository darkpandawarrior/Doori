package com.mileway.feature.logging.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.JustificationReason
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_affidavit_required
import com.mileway.core.ui.resources.logging_exceptions_editable_only
import com.mileway.core.ui.resources.logging_exceptions_save
import com.mileway.core.ui.resources.logging_exceptions_unsaved
import com.mileway.core.ui.resources.logging_justification_required
import com.mileway.feature.advances.reconcile.AdvanceReconciliation
import com.mileway.feature.logging.affidavit.AffidavitField
import com.mileway.feature.logging.justification.JustificationReasonPicker
import org.jetbrains.compose.resources.stringResource

/** Reviews policy for the entire persisted report and exposes guarded submit/recall actions. */
@Composable
fun ReportSubmitScreen(
    reportId: String,
    viewModel: ReportSubmitViewModel,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(reportId) { viewModel.open(reportId) }
    LaunchedEffect(state.screen.dataOrNull?.report?.id) { viewModel.suggestReasons() }
    ReportSubmitContent(
        state, onBack, viewModel::submit, viewModel::recall, viewModel::acceptWarnings, viewModel::retry, onEdit,
        onSaveException = viewModel::saveExceptionDetails,
        onExceptionChanged = viewModel::markExceptionChanged,
    )
}

@Composable
private fun ReportSubmitContent(
    state: ReportSubmitViewModel.State,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onRecall: () -> Unit,
    onAcceptWarnings: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSaveException: (String, Boolean, String, JustificationReason?, String) -> Unit = { _, _, _, _, _ -> },
    onExceptionChanged: (String) -> Unit = {},
) {
    val review = state.screen.dataOrNull
    val submitted = review?.report?.state == ReportLifecycleState.SUBMITTED
    FormSubmissionScaffold(
        title = "Review expense report",
        onBack = onBack,
        onSubmit = if (submitted) onRecall else onSubmit,
        submitLabel = if (submitted) "Recall report" else "Submit report",
        canSubmit = if (submitted) review.canRecall else review?.canSubmit == true && state.pendingExceptionIds.isEmpty(),
        isSubmitting = state.busy,
    ) { padding ->
        ScreenStateContent(state.screen, modifier = Modifier.padding(padding).padding(16.dp), onRetry = onRetry) { content ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(content.report.state.name, style = MaterialTheme.typography.titleMedium)
                if (content.report.lines.none { it is AdvanceLine } && content.hardFlags.none { it.code in setOf("INVALID_TOTAL", "INVALID_REPORT") }) {
                    Text(reportTotalsLabel(content.report))
                }
                content.reconciliation?.let { reconciliation ->
                    Text("Reconcilable spend · ${formatMinorCurrency(reconciliation.spendMinor, reconciliation.currency)}")
                    Text("Applied advances · ${formatMinorCurrency(reconciliation.appliedMinor, reconciliation.currency)}")
                    Text(reconciliationLabel(reconciliation), style = MaterialTheme.typography.titleMedium)
                    reconciliation.excluded.forEach { Text("Excluded ${it.lineId}: ${it.reason}") }
                }
                if (content.report.isEditable && content.report.lines.all { it is ExpenseLine } && onEdit != null) {
                    TextButton(onClick = onEdit, enabled = !state.busy) { Text("Edit grouped items") }
                }
                state.error?.let { error ->
                    val message = when (error) {
                        "EXCEPTIONS_EDITABLE_ONLY" -> stringResource(Res.string.logging_exceptions_editable_only)
                        "EXCEPTIONS_UNSAVED" -> stringResource(Res.string.logging_exceptions_unsaved)
                        else -> error
                    }
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                ReportClaimItems(content.report.lines)
                content.report.lines.filterIsInstance<ExpenseLine>().forEach { line ->
                    ExpenseExceptionEditor(
                        line = line,
                        requiresAffidavit = line.id in content.requiredAffidavitIds,
                        suggestion = state.suggestions[line.id],
                        enabled = content.report.isEditable && !state.busy,
                        onSave = onSaveException,
                        onChanged = onExceptionChanged,
                    )
                }
                Text("Report policy", style = MaterialTheme.typography.titleMedium)
                if (content.hardFlags.isEmpty() && content.softFlags.isEmpty()) Text("No policy flags")
                content.hardFlags.forEach { flag ->
                    val message = when (flag.code) {
                        "AFFIDAVIT_REQUIRED" -> stringResource(Res.string.logging_affidavit_required, flag.message)
                        "JUSTIFICATION_REQUIRED" -> stringResource(Res.string.logging_justification_required, flag.message)
                        else -> "Blocked: ${flag.message}"
                    }
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                content.softFlags.forEach { flag -> Text("Warning: ${flag.message}") }
                if (content.softFlags.isNotEmpty() && content.report.isEditable) {
                    Row {
                        Checkbox(checked = content.warningsAccepted, onCheckedChange = onAcceptWarnings, enabled = !state.busy)
                        Text("I have reviewed the policy warnings", modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (submitted) {
                    Text(if (content.canRecall) "You can recall before an approver acts" else "An approval action has been recorded; recall is unavailable")
                }
            }
        }
    }
}

@Composable
private fun ExpenseExceptionEditor(
    line: ExpenseLine,
    requiresAffidavit: Boolean,
    suggestion: JustificationReason?,
    enabled: Boolean,
    onSave: (String, Boolean, String, JustificationReason?, String) -> Unit,
    onChanged: (String) -> Unit,
) {
    var accepted by remember(line) { mutableStateOf(line.affidavitAccepted) }
    var affidavitNote by remember(line) { mutableStateOf(line.affidavitNote) }
    var reason by remember(line) { mutableStateOf(line.justificationReason) }
    var justificationNote by remember(line) { mutableStateOf(line.justificationNote) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.merchant, style = MaterialTheme.typography.titleSmall)
        if (requiresAffidavit) {
            AffidavitField(
                accepted, affidavitNote,
                onAcceptedChange = { accepted = it; onChanged(line.id) },
                onNoteChange = { affidavitNote = it; onChanged(line.id) },
                enabled = enabled,
            )
        }
        JustificationReasonPicker(
            reason, justificationNote,
            onReasonChange = { reason = it; onChanged(line.id) },
            onNoteChange = { justificationNote = it; onChanged(line.id) },
            suggestion = suggestion,
            enabled = enabled,
        )
        TextButton(
            onClick = { onSave(line.id, accepted, affidavitNote, reason, justificationNote) },
            enabled = enabled,
        ) { Text(stringResource(Res.string.logging_exceptions_save)) }
    }
}

@Composable
private fun ReportClaimItems(lines: List<ClaimLine>) {
    lines.forEach { line ->
        val label =
            when (line) {
                is ExpenseLine -> line.merchant
                is PerDiemLine -> line.incurredOn?.let { "Per diem · $it" } ?: "Per diem"
                is AdvanceLine -> "Advance ${line.advanceId}"
                else -> "Claim item"
            }
        Text(if (line is AdvanceLine) label else "$label · ${formatMinorCurrency(line.amountMinor, line.currency)}")
        if (line is ExpenseLine) FxRateLabel(line)
    }
}

@Composable
private fun FxRateLabel(line: ExpenseLine) {
    if (line.currency == "INR") return
    Text(line.fxRate?.description() ?: "No FX pin: amount checks skipped; enter a manual rate (approximate).")
}

@Preview
@Composable
private fun ReportSubmitPreview() {
    ReportSubmitContent(
        state = ReportSubmitViewModel.State(screen = ScreenState.Content(ReportSubmitViewModel.Review(Report("demo", "employee"), emptyList(), emptyList()))),
        onBack = {},
        onSubmit = {},
        onRecall = {},
        onAcceptWarnings = {},
        onRetry = {},
    )
}

internal fun reportTotalsLabel(report: Report): String = "${report.lines.size} items · ${formatMinorCurrency(report.totalAmountMinor(), report.currency())}"

internal fun reconciliationLabel(result: AdvanceReconciliation): String {
    val label =
        when {
            result.netMinor > 0 -> "Owed to employee"
            result.netMinor < 0 -> "Owed back"
            else -> "Nothing owed"
        }
    val amount = if (result.netMinor < 0) -result.netMinor else result.netMinor
    val partial = if (result.excluded.isEmpty()) "" else " (reconcilable lines only)"
    return "$label$partial · ${formatMinorCurrency(amount, result.currency)}"
}
