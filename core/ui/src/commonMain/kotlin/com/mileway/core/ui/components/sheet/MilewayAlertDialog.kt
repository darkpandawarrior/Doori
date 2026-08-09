package com.mileway.core.ui.components.sheet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
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
            TextButton(onClick = onConfirm, shape = DesignTokens.Shape.button) {
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
