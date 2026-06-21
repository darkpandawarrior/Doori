package com.miletracker.core.ui.components.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tone for [ActionConfirmationBottomSheet]. Drives the
 * header icon + the confirm-button colour so confirmations read consistently across the app.
 */
enum class ActionConfirmationToneType { Success, Danger, Warning, Info }

/** Resolved colours + icon for a [ActionConfirmationToneType]. */
data class ActionConfirmationTonePalette(
    val icon: ImageVector,
    val primaryColor: Color,
    val onPrimaryColor: Color,
    val containerColor: Color,
)

@Composable
fun rememberTonePalette(tone: ActionConfirmationToneType): ActionConfirmationTonePalette {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        ActionConfirmationToneType.Success ->
            ActionConfirmationTonePalette(Icons.Filled.CheckCircle, Color(0xFF12B76A), Color.White, Color(0xFF12B76A))
        ActionConfirmationToneType.Danger ->
            ActionConfirmationTonePalette(Icons.Filled.Warning, scheme.error, scheme.onError, scheme.error)
        ActionConfirmationToneType.Warning ->
            ActionConfirmationTonePalette(Icons.Filled.Warning, Color(0xFFF59E0B), Color.White, Color(0xFFF59E0B))
        ActionConfirmationToneType.Info ->
            ActionConfirmationTonePalette(Icons.Filled.Info, scheme.primary, scheme.onPrimary, scheme.primary)
    }
}

/**
 * Canonical confirmation bottom sheet. Tone-aware icon header, centered title +
 * description, an optional (mandatory-capable) remarks field, an optional extra [content] slot, and a
 * primary/secondary button pair. Replaces confirmation `AlertDialog`s app-wide.
 *
 * @param onConfirm receives the entered remarks (empty when the remarks field is hidden).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmationBottomSheet(
    title: String,
    onConfirm: (remarks: String) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    icon: ImageVector = Icons.AutoMirrored.Filled.Help,
    tone: ActionConfirmationToneType = ActionConfirmationToneType.Success,
    showRemarksField: Boolean = false,
    isRemarksMandatory: Boolean = false,
    remarksPlaceholder: String = "Enter remarks",
    maxRemarksLength: Int = 500,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = rememberTonePalette(tone)
    var remarks by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    val canConfirm = !isRemarksMandatory || remarks.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(palette.containerColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(46.dp).background(palette.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = palette.onPrimaryColor, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (showRemarksField) {
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = remarks,
                    onValueChange = {
                        if (it.length <= maxRemarksLength) {
                            remarks = it
                            showValidationError = false
                        }
                    },
                    label = { Text(remarksPlaceholder + if (isRemarksMandatory) " *" else "") },
                    isError = showValidationError,
                    supportingText = {
                        if (showValidationError) {
                            Text("Remarks are required for this action", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${remarks.length}/$maxRemarksLength")
                        }
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            content?.let {
                Spacer(Modifier.height(16.dp))
                it()
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isRemarksMandatory && remarks.isBlank()) showValidationError = true else onConfirm(remarks)
                },
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = palette.primaryColor,
                        contentColor = palette.onPrimaryColor,
                    ),
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(dismissLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Bulk action verbs for [BulkActionConfirmationBottomSheet] / [BulkActionProgressBottomSheet]. */
enum class BulkActionType { Approve, Decline, Hold, Unhold }

/** Confirmation for a multi-select bulk operation. */
@Composable
fun BulkActionConfirmationBottomSheet(
    actionType: BulkActionType,
    selectedCount: Int,
    onConfirm: (remarks: String) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = actionType.name,
    isRemarksMandatory: Boolean = actionType == BulkActionType.Decline || actionType == BulkActionType.Hold,
) {
    val tone =
        when (actionType) {
            BulkActionType.Decline -> ActionConfirmationToneType.Danger
            BulkActionType.Hold -> ActionConfirmationToneType.Warning
            else -> ActionConfirmationToneType.Success
        }
    val icon =
        when (actionType) {
            BulkActionType.Approve -> Icons.Filled.CheckCircle
            BulkActionType.Decline -> Icons.Filled.Warning
            BulkActionType.Hold -> Icons.Filled.Warning
            BulkActionType.Unhold -> Icons.Filled.Info
        }
    ActionConfirmationBottomSheet(
        title = "${actionType.name} $selectedCount item${if (selectedCount == 1) "" else "s"}",
        description = "The same remarks will be applied to all selected items.",
        confirmLabel = confirmLabel,
        icon = icon,
        tone = tone,
        showRemarksField = true,
        isRemarksMandatory = isRemarksMandatory,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** Live progress for a running bulk operation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionProgressBottomSheet(
    title: String,
    total: Int,
    completed: Int,
    failed: Int,
    inProgress: Boolean,
    onDismiss: () -> Unit,
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                "$completed of $total completed${if (failed > 0) " · $failed failed" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!inProgress) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(8.dp)) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
