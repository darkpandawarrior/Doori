package com.mileway.feature.advances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_success_body
import com.mileway.core.ui.resources.advances_success_title
import com.mileway.core.ui.resources.core_action_done
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/*
 * PLAN_V35.P4: pieces shared by AskAdvanceFormScreen and QrRequestFormScreen — a labelled
 * dropdown (rung 2: mirrors CreatePurchaseRequestScreen's OfficeLocationDropdown), a labelled
 * text field with inline error support, a declaration checkbox, and the shared success state.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LabelledDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            isError = isError,
            label = { Text(label) },
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun LabelledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun DeclarationCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Opens a [WheelDatePickerDialog] on tap; renders the picked date, or [label] when unset. */
@Composable
internal fun DatePickerField(
    label: String,
    valueMs: Long?,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
        Text(valueMs?.let { formatDate(it) } ?: label)
    }
    if (showPicker) {
        WheelDatePickerDialog(
            initialDateMillis = valueMs,
            onConfirm = {
                onPick(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
internal fun RequestSuccessContent(
    permissionId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.advances_success_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(Res.string.advances_success_body, permissionId),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = DesignTokens.Spacing.s),
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = DesignTokens.Spacing.xl), shape = DesignTokens.Shape.button) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Text(stringResource(Res.string.core_action_done), modifier = Modifier.padding(start = DesignTokens.Spacing.s))
        }
    }
}
