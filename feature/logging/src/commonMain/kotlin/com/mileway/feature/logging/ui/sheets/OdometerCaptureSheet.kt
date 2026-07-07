package com.mileway.feature.logging.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_attach_odometer_photo
import com.mileway.core.ui.resources.logging_cancel
import com.mileway.core.ui.resources.logging_confirm
import com.mileway.core.ui.resources.logging_odometer_capture_body
import com.mileway.core.ui.resources.logging_odometer_end
import com.mileway.core.ui.resources.logging_odometer_photo_retake
import com.mileway.core.ui.resources.logging_odometer_reading_label
import com.mileway.core.ui.resources.logging_odometer_start
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.ui.screens.rememberReceiptAttachmentLauncher
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * P5.3: start/end odometer capture sheet, gated behind [com.mileway.core.data.settings
 * .DemoSettingsRepository.logMilesOdometerCaptureEnabled]. Reuses [AppActionSheet]'s modal styling
 * and the existing [rememberReceiptAttachmentLauncher] photo picker (no new camera/OCR dependency —
 * this demo's offline scope doesn't need on-device OCR the way `feature/tracking`'s live-trip
 * capture does; a typed reading is the [OdometerReadingSource.MANUAL] path here).
 *
 * @param purpose        which reading this sheet is collecting
 * @param existing       a previously captured reading for this purpose, if editing
 * @param onCaptured     confirmed reading + photo (may be a placeholder path if the user skipped photo)
 * @param onDismiss       called when the sheet is dismissed without confirming
 */
@Composable
fun OdometerCaptureSheet(
    purpose: OdometerPurpose,
    existing: OdometerCaptureResult?,
    onCaptured: (OdometerCaptureResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var reading by remember { mutableStateOf(existing?.reading?.toString().orEmpty()) }
    var photoUri by remember { mutableStateOf(existing?.imageUri.orEmpty()) }
    val launchPicker =
        rememberReceiptAttachmentLauncher(onPicked = { uri -> photoUri = uri })

    val parsed = reading.toIntOrNull()
    val titleText =
        if (purpose == OdometerPurpose.START) {
            stringResource(Res.string.logging_odometer_start)
        } else {
            stringResource(Res.string.logging_odometer_end)
        }

    AppActionSheet(
        onDismiss = onDismiss,
        title = titleText,
    ) {
        Text(
            stringResource(Res.string.logging_odometer_capture_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = reading,
            onValueChange = { text -> if (text.length <= 7) reading = text.filter { it.isDigit() } },
            label = { Text(stringResource(Res.string.logging_odometer_reading_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = launchPicker, modifier = Modifier.fillMaxWidth()) {
            Text(if (photoUri.isBlank()) stringResource(Res.string.logging_attach_odometer_photo) else stringResource(Res.string.logging_odometer_photo_retake))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.logging_cancel)) }
            Button(
                onClick = {
                    val value = parsed ?: return@Button
                    onCaptured(
                        OdometerCaptureResult(
                            purpose = purpose,
                            imageUri = photoUri,
                            reading = value,
                            source = OdometerReadingSource.MANUAL,
                            captureTimeMs = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                },
                enabled = parsed != null && parsed >= 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.logging_confirm)) }
        }
    }
}
