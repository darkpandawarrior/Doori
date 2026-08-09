package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_close
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Tier 1 of the three-tier sheet system (see [MilewayPickerSheet], [MilewayAlertDialog]): an
 * immediate, blocking or destructive decision — consent, pause reason, policy violation, session
 * restore. Grabber handle, bold title, and **at most two actions** ([validateActionSheetActions]
 * enforces this, not just convention). When [guardDismissal] is true, an implicit dismissal
 * (swipe-down, scrim tap, back press) is intercepted and routed through a discard-confirmation —
 * losing a recorded drive to a stray swipe is unacceptable in this product. Tapping an explicit
 * [actions] button always bypasses the guard: that's a deliberate choice, not an accident.
 *
 * Takes plain data + lambdas only (no ViewModel), so the gallery can render it directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilewayActionSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: List<MilewaySheetAction> = emptyList(),
    guardDismissal: Boolean = false,
    discardTitle: String = "",
    discardBody: String = "",
    discardConfirmLabel: String = "",
    discardCancelLabel: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    validateActionSheetActions(actions)
    require(!guardDismissal || (discardTitle.isNotBlank() && discardConfirmLabel.isNotBlank())) {
        "MilewayActionSheet(guardDismissal = true) requires discardTitle + discardConfirmLabel copy."
    }

    var showDiscardConfirm by remember { mutableStateOf(false) }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            // The single source of truth for the guard: vetoes the Hidden transition for both the
            // drag gesture and the scrim/back-triggered animateToDismiss, so neither path silently
            // discards. See sheetShouldVetoDismiss's doc for why this must stay a pure function.
            confirmValueChange = { target ->
                if (sheetShouldVetoDismiss(target, guardDismissal)) {
                    showDiscardConfirm = true
                    false
                } else {
                    true
                }
            },
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                // An explicit, screen-reader-reachable close affordance — swipe-to-dismiss alone
                // isn't reliably accessible (switch access, some TalkBack gestures). Routes through
                // the same guard as the gesture path, not a bypass.
                IconButton(
                    onClick = {
                        if (sheetShouldVetoDismiss(SheetValue.Hidden, guardDismissal)) {
                            showDiscardConfirm = true
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.heightIn(min = DesignTokens.IconSize.minTouchTarget),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.core_cd_close))
                }
            }

            content()

            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    actions.forEachIndexed { index, action ->
                        val buttonModifier = Modifier.fillMaxWidth().heightIn(min = DesignTokens.IconSize.minTouchTarget)
                        if (index == 0) {
                            Button(
                                onClick = action.onClick,
                                modifier = buttonModifier,
                                shape = DesignTokens.Shape.button,
                                colors =
                                    if (action.isDestructive) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError,
                                        )
                                    } else {
                                        ButtonDefaults.buttonColors()
                                    },
                            ) {
                                Text(action.label, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(onClick = action.onClick, modifier = buttonModifier, shape = DesignTokens.Shape.button) {
                                Text(action.label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiscardConfirm) {
        MilewayAlertDialog(
            title = discardTitle,
            message = discardBody,
            confirmLabel = discardConfirmLabel,
            onConfirm = {
                showDiscardConfirm = false
                onDismiss()
            },
            isMandatory = false,
            dismissLabel = discardCancelLabel.ifBlank { null },
            onDismiss = { showDiscardConfirm = false },
        )
    }
}

/** One action button in a [MilewayActionSheet]. Index 0 renders as the filled primary; index 1 (if present) as outlined secondary. */
data class MilewaySheetAction(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
)

private const val MAX_ACTION_SHEET_ACTIONS = 2

/**
 * Enforces the taxonomy rule: [MilewayActionSheet] is an immediate blocking/destructive decision
 * with MAX TWO actions, never a menu. Pulled out as a pure function — rather than an inline
 * `require` only reachable through composition — so the rule is unit-testable without Compose.
 */
fun validateActionSheetActions(actions: List<MilewaySheetAction>) {
    require(actions.size <= MAX_ACTION_SHEET_ACTIONS) {
        "MilewayActionSheet allows at most $MAX_ACTION_SHEET_ACTIONS actions, got ${actions.size}."
    }
}

/**
 * The dismiss-guard decision at the heart of [MilewayActionSheet]: whether a transition to
 * [target] must be vetoed rather than allowed through. Only the terminal [SheetValue.Hidden]
 * target is ever worth guarding — [SheetValue.Expanded] is just the sheet settling open, not a
 * data-loss risk. Kept as a pure function of (target, guardDismissal) so it's callable from both
 * the gesture-driven `confirmValueChange` and the explicit close button without duplicating the
 * rule, and so it's unit-testable without spinning up Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun sheetShouldVetoDismiss(
    target: SheetValue,
    guardDismissal: Boolean,
): Boolean = guardDismissal && target == SheetValue.Hidden
