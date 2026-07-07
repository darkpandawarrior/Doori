package com.mileway.feature.media.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.media.model.OcrResult

/**
 * Material3 bottom sheet presenting a mocked [OcrResult]: the detected odometer
 * (large), confidence as a percent, and a "Watermarked" success badge.
 *
 * @param onConfirm  user accepts the reading (-> confirm the pending attachment)
 * @param onEdit     user wants to tweak the reading (demo: just dismiss)
 * @param onDismiss  sheet dismissed via scrim / drag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultBottomSheet(
    result: OcrResult,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = DesignTokens.Shape.sheet,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.Spacing.xl,
                        vertical = DesignTokens.Spacing.l,
                    ),
        ) {
            Text(
                text = "OCR result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.l))

            Text(
                text = "Detected odometer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = result.detectedOdometer ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Confidence ${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(DesignTokens.Spacing.l))
                if (result.watermarkApplied) {
                    WatermarkBadge()
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))

            Text(
                text = result.rawText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text("Edit")
                }
                Button(
                    onClick = onConfirm,
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text("Confirm")
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))
        }
    }
}

@Composable
private fun WatermarkBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(DesignTokens.StatusColors.success.copy(alpha = 0.15f))
                .padding(
                    horizontal = DesignTokens.Spacing.m,
                    vertical = DesignTokens.Spacing.xs,
                ),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DesignTokens.StatusColors.success,
            modifier = Modifier.size(DesignTokens.IconSize.inline),
        )
        Spacer(Modifier.width(DesignTokens.Spacing.xs))
        Text(
            text = "Watermarked",
            style = MaterialTheme.typography.labelMedium,
            color = DesignTokens.StatusColors.success,
            fontWeight = FontWeight.Medium,
        )
    }
}
