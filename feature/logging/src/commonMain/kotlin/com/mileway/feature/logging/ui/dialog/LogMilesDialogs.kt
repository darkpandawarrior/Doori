package com.mileway.feature.logging.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_accept_resubmit
import com.mileway.core.ui.resources.logging_amount_adjusted_body
import com.mileway.core.ui.resources.logging_amount_adjusted_title
import com.mileway.core.ui.resources.logging_calculated
import com.mileway.core.ui.resources.logging_cancel
import com.mileway.core.ui.resources.logging_claimed
import com.mileway.core.ui.resources.logging_close
import com.mileway.core.ui.resources.logging_current
import com.mileway.core.ui.resources.logging_done
import com.mileway.core.ui.resources.logging_plural_submission_violations
import com.mileway.core.ui.resources.logging_policy_violations_title
import com.mileway.core.ui.resources.logging_reimbursable
import com.mileway.core.ui.resources.logging_remarks_label
import com.mileway.core.ui.resources.logging_resubmit_with_remarks
import com.mileway.core.ui.resources.logging_save
import com.mileway.core.ui.resources.logging_submission_blocked_body
import com.mileway.core.ui.resources.logging_submission_blocked_title
import com.mileway.core.ui.resources.logging_tag_employees_title
import com.mileway.core.ui.resources.logging_update_distance_label
import com.mileway.core.ui.resources.logging_verify_distance_body
import com.mileway.core.ui.resources.logging_verify_distance_title
import com.mileway.core.ui.resources.logging_violations_fallback
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Verify Distance" dialog. Shows the auto-calculated distance against the value
 * currently in use, and lets the user override it with a manually entered figure.
 *
 * @param calculatedKm great-circle distance computed from the itinerary
 * @param currentKm    distance currently applied (may already be an override)
 * @param onSave       called with the confirmed distance in km
 * @param onDismiss    called when the dialog is cancelled
 */
@Composable
fun VerifyDistanceDialog(
    calculatedKm: Double,
    currentKm: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentKm.formatDecimal(2)) }
    val parsed = text.toDoubleOrNull()

    AppActionSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.logging_verify_distance_title),
    ) {
        Text(
            stringResource(Res.string.logging_verify_distance_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = DesignTokens.Shape.roundedMd,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledValue(stringResource(Res.string.logging_calculated), "${calculatedKm.formatDecimal(2)} km")
                LabeledValue(stringResource(Res.string.logging_current), "${currentKm.formatDecimal(2)} km", alignEnd = true)
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(Res.string.logging_update_distance_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_cancel)) }
            Button(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null && parsed >= 0.0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.logging_save)) }
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Multi-select dialog for tagging teammates onto a journey. Backed by a local
 * roster of names; confirms the chosen subset via [onConfirm].
 *
 * @param allEmployees     selectable names
 * @param initiallySelected names already tagged
 */
@Composable
fun TaggedEmployeesDialog(
    allEmployees: List<String>,
    initiallySelected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected: SnapshotStateList<String> = remember { initiallySelected.toMutableStateList() }

    AppActionSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.logging_tag_employees_title),
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            allEmployees.forEach { name ->
                val isChecked = name in selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = DesignTokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            if (it) selected.add(name) else selected.remove(name)
                        },
                    )
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_cancel)) }
            Button(onClick = { onConfirm(selected.toList()) }, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_done)) }
        }
    }
}

/**
 * Policy-violation resolution dialog shown after submit when the response carries a non-clean
 * [SubmissionStatus] (P5.4). Branches on `response.submissionStatus` instead of showing a flat
 * message list with a single "Acknowledge" button, since [SubmissionStatus]/[com.mileway.core
 * .data.model.network.PolicyViolation]/[com.mileway.core.data.model.network.ViolationSeverity]
 * already model three distinct severity tiers:
 * - `REIMBURSABLE_ADJUSTED`: informational only — shows the claimed-vs-reimbursable amount and
 *   an "Accept & Resubmit" button that resubmits with no remarks required.
 * - `POLICY_VIOLATION`: lists the violations and requires a non-blank remarks note before
 *   "Resubmit with Remarks" is enabled.
 * - `HARD_STOP`: terminal — lists the violations with no resubmit path, only "Close".
 *
 * One composable, one `when` — deliberately not a separate bottom-sheet-per-severity.
 *
 * @param response      the submission response driving the branch
 * @param onResubmit    invoked with the (possibly blank, for REIMBURSABLE_ADJUSTED) remarks text
 *                       when the user chooses to resubmit; not invoked for HARD_STOP
 * @param onDismiss     invoked when the dialog is dismissed without resubmitting
 */
@Composable
fun ViolationDialog(
    response: ExpenseSubmissionResponse,
    onResubmit: (remarks: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val violationsFallback = stringResource(Res.string.logging_violations_fallback)
    val messages: List<String> =
        buildList {
            response.violations.forEach { add(it.message.ifBlank { it.title }) }
            response.policyViolations.orEmpty().forEach { it.error?.let(::add) }
        }.filter { it.isNotBlank() }.ifEmpty { listOf(violationsFallback) }

    when (response.submissionStatus) {
        SubmissionStatus.REIMBURSABLE_ADJUSTED ->
            ReimbursableAdjustedContent(response = response, onAccept = { onResubmit("") }, onDismiss = onDismiss)
        SubmissionStatus.HARD_STOP ->
            HardStopContent(messages = messages, onDismiss = onDismiss)
        // POLICY_VIOLATION and any other non-clean status fall back to the remarks-gated path.
        else ->
            PolicyViolationContent(messages = messages, onResubmit = onResubmit, onDismiss = onDismiss)
    }
}

/** REIMBURSABLE_ADJUSTED: amount comparison, no remarks required to resubmit. */
@Composable
private fun ReimbursableAdjustedContent(
    response: ExpenseSubmissionResponse,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reimbursable = response.reimbursableAmount ?: response.amount ?: 0.0
    val claimed = response.amount ?: reimbursable

    AppActionSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.logging_amount_adjusted_title),
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = DesignTokens.StatusColors.warning,
        )
        Text(
            stringResource(Res.string.logging_amount_adjusted_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = DesignTokens.Shape.roundedMd,
            color = DesignTokens.StatusColors.warning.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledValue(stringResource(Res.string.logging_claimed), claimed.formatDecimal(2))
                LabeledValue(stringResource(Res.string.logging_reimbursable), reimbursable.formatDecimal(2), alignEnd = true)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_cancel)) }
            Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_accept_resubmit)) }
        }
    }
}

/** POLICY_VIOLATION: violation list plus a required remarks field gating resubmit. */
@Composable
private fun PolicyViolationContent(
    messages: List<String>,
    onResubmit: (remarks: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var remarks by remember { mutableStateOf("") }

    AppActionSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.logging_policy_violations_title),
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = DesignTokens.StatusColors.error,
        )
        Text(
            pluralStringResource(Res.plurals.logging_plural_submission_violations, messages.size, messages.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        messages.forEach { msg ->
            Row(verticalAlignment = Alignment.Top) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(msg, style = MaterialTheme.typography.bodyMedium)
            }
        }
        OutlinedTextField(
            value = remarks,
            onValueChange = { remarks = it },
            label = { Text(stringResource(Res.string.logging_remarks_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_cancel)) }
            Button(
                onClick = { onResubmit(remarks) },
                enabled = remarks.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.logging_resubmit_with_remarks)) }
        }
    }
}

/** HARD_STOP: terminal — violation list, no resubmit path. */
@Composable
private fun HardStopContent(
    messages: List<String>,
    onDismiss: () -> Unit,
) {
    AppActionSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.logging_submission_blocked_title),
    ) {
        Icon(
            Icons.Filled.Block,
            contentDescription = null,
            tint = DesignTokens.StatusColors.error,
        )
        Text(
            stringResource(Res.string.logging_submission_blocked_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        messages.forEach { msg ->
            Row(verticalAlignment = Alignment.Top) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(msg, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.logging_close)) }
    }
}
