package com.mileway.feature.logging.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.network.model.PolicyViolation
import com.mileway.core.network.model.ViolationSeverity
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_approval_required
import com.mileway.core.ui.resources.logging_expense_requires_approval
import com.mileway.core.ui.resources.logging_policy_sheet_review
import com.mileway.core.ui.resources.logging_policy_sheet_submit_anyway
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * V27 P27.E.3: the expense flow's second validation channel — a `ModalBottomSheet` (mirrors
 * DiCE's `PolicyViolationBottomSheet`) shown for the tiered-policy outcome on submit, replacing
 * the old behaviour of silently submitting regardless of the policy result. Per-field errors
 * (category/amount/merchant/office/custom-form) stay inline on the form itself — this sheet is
 * reached only once those already passed (see [com.mileway.feature.logging.viewmodel
 * .ExpenseViewModel.submitExpense]). The live, non-blocking policy preview banner shown while
 * typing on step 2 is unchanged and separate from this sheet.
 *
 * @param violations The policy violations for the current submit attempt (may be empty for a
 *   NEEDS_APPROVAL/HARD_STOP outcome that carries no itemised violation).
 * @param onSubmitAnyway Emitted when the user acknowledges and proceeds with submission.
 * @param onReview Emitted when the user chooses to go back and edit instead.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down) — same intent as [onReview].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensePolicyViolationSheet(
    violations: List<PolicyViolation>,
    onSubmitAnyway: () -> Unit,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(DesignTokens.Shape.button)
                            .background(DesignTokens.StatusColors.warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = DesignTokens.StatusColors.warning,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                }
                Text(
                    text = stringResource(Res.string.logging_approval_required),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    if (violations.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.logging_expense_requires_approval),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        violations.forEach { violation -> ViolationLine(violation) }
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            Button(onClick = onSubmitAnyway, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                Text(stringResource(Res.string.logging_policy_sheet_submit_anyway), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                Text(stringResource(Res.string.logging_policy_sheet_review))
            }
        }
    }
}

@Composable
private fun ViolationLine(violation: PolicyViolation) {
    val tint =
        if (violation.severity == ViolationSeverity.HARDSTOP) {
            MaterialTheme.colorScheme.error
        } else {
            DesignTokens.StatusColors.warning
        }
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(DesignTokens.IconSize.badge),
        )
        Text(
            text = violation.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
