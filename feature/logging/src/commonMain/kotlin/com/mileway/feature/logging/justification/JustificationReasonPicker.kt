package com.mileway.feature.logging.justification

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mileway.core.data.domain.claim.JustificationReason
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_reason_business_necessity
import com.mileway.core.ui.resources.logging_reason_clear
import com.mileway.core.ui.resources.logging_reason_client_request
import com.mileway.core.ui.resources.logging_reason_hint
import com.mileway.core.ui.resources.logging_reason_no_alternative
import com.mileway.core.ui.resources.logging_reason_note
import com.mileway.core.ui.resources.logging_reason_other
import com.mileway.core.ui.resources.logging_reason_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Offline enum picker with an optional hint that only selects a value when tapped. */
@Composable
fun JustificationReasonPicker(
    reason: JustificationReason?,
    note: String,
    onReasonChange: (JustificationReason?) -> Unit,
    onNoteChange: (String) -> Unit,
    suggestion: JustificationReason? = null,
    enabled: Boolean = true,
) {
    Column {
        Text(stringResource(Res.string.logging_reason_title))
        suggestion?.let { hint ->
            TextButton(onClick = { onReasonChange(hint) }, enabled = enabled) {
                Text(stringResource(Res.string.logging_reason_hint, stringResource(hint.labelResource())))
            }
        }
        JustificationReason.entries.forEach { value ->
            FilterChip(
                selected = value == reason,
                onClick = { onReasonChange(value) },
                enabled = enabled,
                label = { Text(stringResource(value.labelResource())) },
            )
        }
        TextButton(onClick = { onReasonChange(null) }, enabled = enabled) { Text(stringResource(Res.string.logging_reason_clear)) }
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            enabled = enabled,
            label = { Text(stringResource(Res.string.logging_reason_note)) },
            isError = reason == JustificationReason.OTHER && note.isBlank(),
        )
    }
}

private fun JustificationReason.labelResource(): StringResource =
    when (this) {
        JustificationReason.BUSINESS_NECESSITY -> Res.string.logging_reason_business_necessity
        JustificationReason.CLIENT_REQUEST -> Res.string.logging_reason_client_request
        JustificationReason.NO_ALTERNATIVE -> Res.string.logging_reason_no_alternative
        JustificationReason.OTHER -> Res.string.logging_reason_other
    }

@Preview
@Composable
private fun JustificationReasonPickerPreview() {
    JustificationReasonPicker(null, "", {}, {}, suggestion = JustificationReason.CLIENT_REQUEST)
}
