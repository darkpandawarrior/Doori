package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_action_confirm
import com.mileway.core.ui.resources.core_odometer_action_accept_typed
import com.mileway.core.ui.resources.core_odometer_discrepancy_body
import com.mileway.core.ui.resources.core_odometer_discrepancy_title
import com.mileway.core.ui.resources.core_odometer_rejection_title
import com.mileway.core.ui.resources.core_odometer_source_ai
import com.mileway.core.ui.resources.core_odometer_source_device
import com.mileway.core.ui.resources.core_odometer_source_user
import com.mileway.core.ui.resources.tracking_action_retake
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * V26 P26.CONV.3: shown when `core:media`'s `OdometerReconciler` returns a `Discrepancy` verdict —
 * the typed/device-OCR/AI readings disagree beyond tolerance. Accept takes the highest-priority
 * available reading (typed > device OCR > AI OCR); Retake sends the caller back into capture.
 * Built on [ActionConfirmationBottomSheet] so it inherits the app's one confirmation-sheet look.
 */
@Composable
fun OdometerDiscrepancySheet(
    userReading: Int?,
    deviceReading: Int?,
    aiReading: Int?,
    onAccept: (Int) -> Unit,
    onRetake: () -> Unit,
) {
    val best = userReading ?: deviceReading ?: aiReading ?: return
    ActionConfirmationBottomSheet(
        title = stringResource(Res.string.core_odometer_discrepancy_title),
        description = stringResource(Res.string.core_odometer_discrepancy_body),
        confirmLabel = stringResource(Res.string.core_action_confirm),
        dismissLabel = stringResource(Res.string.tracking_action_retake),
        tone = ActionConfirmationToneType.Warning,
        onConfirm = { onAccept(best) },
        onDismiss = onRetake,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
            ) {
                userReading?.let { ReadingRow(stringResource(Res.string.core_odometer_source_user), it) }
                deviceReading?.let { ReadingRow(stringResource(Res.string.core_odometer_source_device), it) }
                aiReading?.let { ReadingRow(stringResource(Res.string.core_odometer_source_ai), it) }
            }
        },
    )
}

/**
 * V26 P26.CONV.3: shown when `OdometerReconciler` returns a `Rejected` verdict — no reading could be
 * determined, or the sources disagreed too wildly to arbitrate. Accept falls back to the user's
 * typed value when one exists (no-op otherwise); Retake sends the caller back into capture.
 */
@Composable
fun OdometerRejectionSheet(
    reason: String,
    userReading: Int?,
    onAccept: (Int) -> Unit,
    onRetake: () -> Unit,
) {
    ActionConfirmationBottomSheet(
        title = stringResource(Res.string.core_odometer_rejection_title),
        description = reason,
        confirmLabel = stringResource(Res.string.core_odometer_action_accept_typed),
        dismissLabel = stringResource(Res.string.tracking_action_retake),
        tone = ActionConfirmationToneType.Danger,
        // ponytail: no typed value to fall back to — the confirm button no-ops rather than plumbing
        // an `enabled` override through the shared ActionConfirmationBottomSheet for this one caller.
        onConfirm = { userReading?.let(onAccept) },
        onDismiss = onRetake,
    )
}

@Composable
private fun ReadingRow(
    label: String,
    reading: Int,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(reading.withThousandsSeparators(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** `String.format("%,d", ...)` is JVM-only — commonMain needs its own thousands-grouping. */
private fun Int.withThousandsSeparators(): String = toString().reversed().chunked(3).joinToString(",").reversed()
