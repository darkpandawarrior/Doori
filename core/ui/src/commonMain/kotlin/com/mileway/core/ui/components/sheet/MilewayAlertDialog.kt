package com.mileway.core.ui.components.sheet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.theme.DesignTokens

/**
 * Tier 3 of the three-tier sheet system (see [MilewayActionSheet], [MilewayPickerSheet]): a
 * blocking error, centred, and ONLY that — never a form, a list, or a routine confirmation (those
 * belong to [MilewayActionSheet]). When [isMandatory] (the default), there is no way out except
 * [confirmLabel]: no cancel button is rendered regardless of [dismissLabel], and scrim tap / back
 * press are no-ops. Set [isMandatory] = false only for a blocking error the user may legitimately
 * shrug off (paired with [dismissLabel] + [onDismiss]).
 *
 * [confirmLabel] always renders as a filled [Button] — never a flat [TextButton] — so the primary
 * action reads as unmistakable rather than as two equal-weight footnotes; [dismissLabel] (when
 * present) stays a ghost [TextButton], the correct lower-emphasis sibling. [isDestructiveConfirm]
 * picks which fill it gets: red for a choice that actually destroys something (discarding a
 * recorded drive), the app's own primary colour for a plain blocking-error acknowledgement — the
 * red [Icons.Filled.ErrorOutline] header icon communicates "this is serious" either way, but only
 * a genuinely irreversible confirm action should also colour the button that triggers it.
 *
 * Takes plain data + lambdas only (no ViewModel), so the gallery can render it directly.
 */
@Composable
fun MilewayAlertDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isMandatory: Boolean = true,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    isDestructiveConfirm: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isMandatory) onDismiss?.invoke() },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                // Decorative: the title already states "this is a blocking error".
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = DesignTokens.Shape.button,
                colors =
                    if (isDestructiveConfirm) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton =
            if (!isMandatory && dismissLabel != null) {
                {
                    TextButton(onClick = { onDismiss?.invoke() }, shape = DesignTokens.Shape.button) {
                        Text(dismissLabel)
                    }
                }
            } else {
                null
            },
    )
}
