@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_retry
import com.mileway.core.ui.resources.core_action_exit
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Non-dismissible error dialog for unrecoverable failures (e.g. corrupt local data, a fatal
 * startup error) where the only sane user actions are to retry or leave the flow entirely.
 *
 * Deliberately has no scrim-tap/back-press dismiss path: [AlertDialog]'s `onDismissRequest` is a
 * no-op, so the dialog only closes via [onRetry] or [onExit].
 */
@Composable
fun CriticalErrorDialog(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissible: retry/exit are the only ways out */ },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(text = title, color = MaterialTheme.colorScheme.onErrorContainer) },
        text = { Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer) },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        confirmButton = {
            TextButton(onClick = onRetry, shape = DesignTokens.Shape.button) {
                Text(text = stringResource(Res.string.action_retry), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onExit, shape = DesignTokens.Shape.button) {
                Text(text = stringResource(Res.string.core_action_exit), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    )
}
