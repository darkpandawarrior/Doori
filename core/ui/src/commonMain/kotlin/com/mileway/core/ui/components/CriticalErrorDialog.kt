@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_retry
import com.mileway.core.ui.resources.core_action_exit
import org.jetbrains.compose.resources.stringResource

/**
 * Non-dismissible error dialog for unrecoverable failures (e.g. corrupt local data, a fatal
 * startup error) where the only sane user actions are to retry or leave the flow entirely.
 *
 * Deliberately has no scrim-tap/back-press dismiss path: [AlertDialog]'s `onDismissRequest` is a
 * no-op, so the dialog only closes via [onRetry] or [onExit].
 *
 * Severity is carried by the [Icons.Filled.Warning] header tint alone. An earlier version washed
 * the whole dialog in `errorContainer` and set title/body text to `onErrorContainer` — measured
 * contrast on this theme's dark surfaces was ~4.2:1, under the 4.5:1 AA floor for body text (title
 * cleared the 3:1 large-text floor, the description didn't). Leaving `containerColor`/text colour
 * unset falls back to `AlertDialogDefaults`' own `surface`/`onSurface`/`onSurfaceVariant` roles,
 * which measure >9:1 on every shipped theme. [onRetry] is the recommended, non-destructive action
 * — it gets [MilewayPrimaryButton], not the error-red tint the old `TextButton` used; red is
 * reserved for actions that destroy data, and retrying isn't one.
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
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            MilewayPrimaryButton(text = stringResource(Res.string.action_retry), onClick = onRetry)
        },
        dismissButton = {
            MilewayTextButton(text = stringResource(Res.string.core_action_exit), onClick = onExit)
        },
    )
}
