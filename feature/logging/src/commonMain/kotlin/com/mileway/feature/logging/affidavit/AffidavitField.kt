package com.mileway.feature.logging.affidavit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_affidavit_confirm
import com.mileway.core.ui.resources.logging_affidavit_note
import org.jetbrains.compose.resources.stringResource

/** Controlled missing-receipt declaration, independent of network and AI settings. */
@Composable
fun AffidavitField(
    accepted: Boolean,
    note: String,
    onAcceptedChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    Column {
        Row {
            Checkbox(checked = accepted, onCheckedChange = onAcceptedChange, enabled = enabled)
            Text(stringResource(Res.string.logging_affidavit_confirm))
        }
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            enabled = enabled,
            label = { Text(stringResource(Res.string.logging_affidavit_note)) },
            isError = note.isBlank(),
        )
    }
}

@Preview
@Composable
private fun AffidavitFieldPreview() {
    AffidavitField(false, "", {}, {})
}
