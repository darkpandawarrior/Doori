package com.mileway.feature.tracking.ui.sheets

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.media.model.OdometerReading
import com.mileway.core.media.ocr.OdometerReconciler
import com.mileway.core.media.ocr.rememberOdometerOcrService
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.components.sheet.OdometerDiscrepancySheet
import com.mileway.core.ui.components.sheet.OdometerRejectionSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_action_confirm
import com.mileway.core.ui.resources.tracking_action_cancel
import com.mileway.core.ui.resources.tracking_action_retake
import com.mileway.core.ui.resources.tracking_odometer_cd_photo
import com.mileway.core.ui.resources.tracking_odometer_end_title
import com.mileway.core.ui.resources.tracking_odometer_enter_end_reading
import com.mileway.core.ui.resources.tracking_odometer_enter_start_reading
import com.mileway.core.ui.resources.tracking_odometer_manual_entry
import com.mileway.core.ui.resources.tracking_odometer_ocr_result
import com.mileway.core.ui.resources.tracking_odometer_reading_km_label
import com.mileway.core.ui.resources.tracking_odometer_reading_km_value
import com.mileway.core.ui.resources.tracking_odometer_start_title
import com.mileway.core.ui.resources.tracking_odometer_use_reading
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * P-E.1/V26 P26.CONV: OCR confirmation sheet for odometer photos, called from `feature:tracking`'s
 * androidMain-only [com.mileway.feature.tracking.ui.screens.OdometerCameraScreen]. OCR runs through
 * `core:media`'s [rememberOdometerOcrService] — the one shared odometer OCR pipeline every feature
 * uses — and a disagreement between the typed/device-OCR/AI readings routes through
 * [OdometerReconciler] into [OdometerDiscrepancySheet]/[OdometerRejectionSheet] instead of silently
 * picking one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerReadingConfirmSheet(
    capturedUri: String,
    purpose: OdometerPurpose,
    baseReading: Int,
    sessionDistanceKm: Double,
    onUseReading: (reading: Int, isManual: Boolean) -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isProcessing by remember { mutableStateOf(true) }
    val detectedReading =
        remember(purpose, baseReading, sessionDistanceKm) {
            if (purpose == OdometerPurpose.START) {
                baseReading
            } else {
                baseReading + sessionDistanceKm.toInt().coerceAtLeast(1)
            }
        }
    var displayedReading by remember { mutableStateOf(detectedReading) }

    var showManualDialog by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf(detectedReading.toString()) }

    // V26 P26.CONV.3: not keyed on capturedUri — survives across retakes within this capture
    // session so OdometerReconciler.withRetake can accumulate retakeCount/retakeHistory correctly.
    var odometerReading by remember { mutableStateOf(OdometerReading()) }
    var verdict by remember { mutableStateOf<OdometerReconciler.Verdict?>(null) }

    val ocrService = rememberOdometerOcrService()
    LaunchedEffect(capturedUri) {
        isProcessing = true
        val aggregate = ocrService.analyzeSingle(capturedUri)
        val aiReading = runCatching { ocrService.analyzeAi(capturedUri) }.getOrNull()
        val reading =
            odometerReading.copy(
                url = capturedUri,
                deviceOcrReading = aggregate.reading?.toString(),
                aiOcrReading = aiReading,
            )
        odometerReading = reading
        val result = OdometerReconciler.reconcile(reading)
        verdict = result
        displayedReading =
            when (result) {
                is OdometerReconciler.Verdict.Accepted -> result.reading
                else -> aggregate.reading ?: detectedReading
            }
        isProcessing = false
    }

    fun retakeWithHistory() {
        odometerReading = OdometerReconciler.withRetake(odometerReading)
        verdict = null
        onRetake()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val titleText =
                if (purpose == OdometerPurpose.START) {
                    stringResource(Res.string.tracking_odometer_start_title)
                } else {
                    stringResource(Res.string.tracking_odometer_end_title)
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (isProcessing) {
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))

            AsyncImage(
                model = capturedUri,
                contentDescription = stringResource(Res.string.tracking_odometer_cd_photo),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(DesignTokens.Shape.roundedSm)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            if (!isProcessing) {
                Text(
                    text = stringResource(Res.string.tracking_odometer_ocr_result),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                Text(
                    text = stringResource(Res.string.tracking_odometer_reading_km_value, "%,d".format(displayedReading)),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    shape = DesignTokens.Shape.button,
                    onClick = { showManualDialog = true },
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.tracking_odometer_manual_entry))
                }
                Spacer(Modifier.height(DesignTokens.Spacing.m))
            } else {
                Spacer(Modifier.height(56.dp))
            }

            val needsReconciliation = verdict is OdometerReconciler.Verdict.Discrepancy || verdict is OdometerReconciler.Verdict.Rejected
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    onClick = ::retakeWithHistory,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.tracking_action_retake)) }
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = { onUseReading(displayedReading, false) },
                    modifier = Modifier.weight(1f),
                    // V26 P26.CONV.3: a Discrepancy/Rejected verdict must be resolved via the
                    // reconciliation sheet below, not silently confirmed with the best-effort guess.
                    enabled = !isProcessing && !needsReconciliation,
                ) { Text(stringResource(Res.string.tracking_odometer_use_reading)) }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.l))
        }
    }

    when (val v = verdict) {
        is OdometerReconciler.Verdict.Discrepancy ->
            OdometerDiscrepancySheet(
                userReading = v.userReading,
                deviceReading = v.deviceReading,
                aiReading = v.aiReading,
                onAccept = { chosen ->
                    displayedReading = chosen
                    verdict = OdometerReconciler.Verdict.Accepted(chosen)
                },
                onRetake = ::retakeWithHistory,
            )
        is OdometerReconciler.Verdict.Rejected ->
            OdometerRejectionSheet(
                reason = v.reason,
                userReading = odometerReading.userReading?.toIntOrNull(),
                onAccept = { chosen ->
                    displayedReading = chosen
                    verdict = OdometerReconciler.Verdict.Accepted(chosen)
                },
                onRetake = ::retakeWithHistory,
            )
        else -> Unit
    }

    if (showManualDialog) {
        AppActionSheet(
            onDismiss = { showManualDialog = false },
            title =
                if (purpose == OdometerPurpose.START) {
                    stringResource(Res.string.tracking_odometer_enter_start_reading)
                } else {
                    stringResource(Res.string.tracking_odometer_enter_end_reading)
                },
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { if (it.length <= 7) manualInput = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(Res.string.tracking_odometer_reading_km_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    onClick = { showManualDialog = false },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.tracking_action_cancel)) }
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = {
                        val parsed = manualInput.toIntOrNull() ?: displayedReading
                        displayedReading = parsed
                        showManualDialog = false
                        onUseReading(parsed, true)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.core_action_confirm)) }
            }
        }
    }
}
