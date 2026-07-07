package com.mileway.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_cancel
import com.mileway.core.ui.resources.settings_language
import com.mileway.core.ui.theme.AppLanguage
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.LocaleController
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Shared (KMP) language picker. Selecting a language calls [LocaleController.setLanguage], which the
 * app-wide `LocalAppLocale` environment observes to instantly re-resolve every `stringResource` on
 * both Android and iOS — no restart. This is the single source of truth for the picker UI; the
 * Android Settings screen and the iOS shell both open it, so the two platforms stay in lock-step.
 *
 * @param onDismiss called after a pick or a cancel so the caller can hide the dialog.
 */
@Composable
fun LanguageSelectionSheet(
    onDismiss: () -> Unit,
    localeController: LocaleController = koinInject(),
) {
    val currentTag by localeController.currentTag.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        selected = language.tag == currentTag,
                        label = language.displayName,
                        onSelect = {
                            localeController.setLanguage(language)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shape = DesignTokens.Shape.button) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun Row(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onSelect)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
