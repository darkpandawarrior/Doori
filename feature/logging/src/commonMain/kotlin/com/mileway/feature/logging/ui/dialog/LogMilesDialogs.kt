package com.mileway.feature.logging.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.theme.DesignTokens

/**
 * "Verify Distance" dialog. Shows the auto-calculated distance against the value
 * currently in use, and lets the user override it with a manually entered figure.
 *
 * @param calculatedKm great-circle distance computed from the itinerary
 * @param currentKm    distance currently applied (may already be an override)
 * @param onSave       called with the confirmed distance in km
 * @param onDismiss    called when the dialog is cancelled
 */
@Composable
fun VerifyDistanceDialog(
    calculatedKm: Double,
    currentKm: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentKm.formatDecimal(2)) }
    val parsed = text.toDoubleOrNull()

    AppActionSheet(
        onDismiss = onDismiss,
        title = "Verify Distance",
    ) {
        Text(
            "We calculated your journey distance. You can keep it or adjust if needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = DesignTokens.Shape.roundedMd,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabeledValue("Calculated", "${calculatedKm.formatDecimal(2)} km")
                LabeledValue("Current", "${currentKm.formatDecimal(2)} km", alignEnd = true)
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Update distance (km)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null && parsed >= 0.0,
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Multi-select dialog for tagging teammates onto a journey. Backed by a local
 * roster of names; confirms the chosen subset via [onConfirm].
 *
 * @param allEmployees     selectable names
 * @param initiallySelected names already tagged
 */
@Composable
fun TaggedEmployeesDialog(
    allEmployees: List<String>,
    initiallySelected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected: SnapshotStateList<String> = remember { initiallySelected.toMutableStateList() }

    AppActionSheet(
        onDismiss = onDismiss,
        title = "Tag Employees",
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            allEmployees.forEach { name ->
                val isChecked = name in selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = DesignTokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            if (it) selected.add(name) else selected.remove(name)
                        },
                    )
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = { onConfirm(selected.toList()) }, modifier = Modifier.weight(1f)) { Text("Done") }
        }
    }
}

/**
 * Policy-violation dialog shown after submit when the response carries violations.
 * Lists the violation messages; the user acknowledges to continue to the success
 * route. Resolves messages from whichever violation shape the response populated.
 */
@Composable
fun ViolationDialog(
    response: ExpenseSubmissionResponse,
    onAcknowledge: () -> Unit,
) {
    val messages: List<String> =
        buildList {
            response.violations.forEach { add(it.message.ifBlank { it.title }) }
            response.policyViolations.orEmpty().forEach { it.error?.let(::add) }
        }.filter { it.isNotBlank() }.ifEmpty { listOf("This submission has policy violations.") }

    AppActionSheet(
        onDismiss = onAcknowledge,
        title = "Policy Violations",
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = DesignTokens.StatusColors.warning,
        )
        Text(
            "Your submission was recorded with ${messages.size} violation${if (messages.size == 1) "" else "s"}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        messages.forEach { msg ->
            Row(verticalAlignment = Alignment.Top) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(msg, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) { Text("Acknowledge") }
    }
}
