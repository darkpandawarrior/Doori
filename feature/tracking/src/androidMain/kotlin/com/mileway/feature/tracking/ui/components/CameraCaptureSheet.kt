package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_close
import com.mileway.core.ui.resources.tracking_action_retake
import com.mileway.core.ui.resources.tracking_capture_cd_photo
import com.mileway.core.ui.resources.tracking_capture_detected_confidence
import com.mileway.core.ui.resources.tracking_capture_reading_odometer
import com.mileway.core.ui.resources.tracking_capture_use_reading
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.media.model.OcrResult
import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.media.ui.camera.CameraCaptureScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

enum class CaptureMode { ODOMETER, PLAIN }

/**
 * Full-screen camera capture overlay reused across the submission flow. Wraps the
 * media module's [CameraCaptureScreen]; in [CaptureMode.ODOMETER] it runs the (mocked)
 * OCR and lets the user confirm the detected reading, in [CaptureMode.PLAIN] it returns
 * the captured uri immediately (e.g. for receipt attachments).
 *
 * Rendered as a conditional composable (NOT a Dialog window) so the CameraX
 * LifecycleCameraController binds to the host screen's lifecycle reliably.
 */
@Composable
fun CameraCaptureSheet(
    mode: CaptureMode,
    onDismiss: () -> Unit,
    onOdometerReading: (String) -> Unit = {},
    onPhotoCaptured: (String) -> Unit = {},
    mediaRepository: MediaRepository = koinInject(),
) {
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var ocr by remember { mutableStateOf<OcrResult?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            val uri = capturedUri
            when {
                // 1. Live camera until a photo is taken.
                uri == null -> {
                    CameraCaptureScreen(
                        onCaptured = { captured ->
                            if (mode == CaptureMode.PLAIN) {
                                onPhotoCaptured(captured)
                                onDismiss()
                            } else {
                                capturedUri = captured
                            }
                        },
                    )
                }
                // 2. Odometer mode: run mocked OCR, then confirm.
                ocr == null -> {
                    LaunchedEffect(uri) { ocr = mediaRepository.runOcr(uri) }
                    CapturedPreview(uri) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(DesignTokens.Spacing.m))
                        Text(stringResource(Res.string.tracking_capture_reading_odometer), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val result = ocr!!
                    CapturedPreview(uri) {
                        Text(
                            text = result.detectedOdometer ?: "—",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(Res.string.tracking_capture_detected_confidence, (result.confidence * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(DesignTokens.Spacing.l))
                        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                            OutlinedButton(
                                shape = DesignTokens.Shape.button,
                                onClick = {
                                    capturedUri = null
                                    ocr = null
                                },
                            ) { Text(stringResource(Res.string.tracking_action_retake)) }
                            Button(
                                shape = DesignTokens.Shape.button,
                                onClick = {
                                    onOdometerReading(result.detectedOdometer.orEmpty())
                                    onDismiss()
                                },
                            ) { Text(stringResource(Res.string.tracking_capture_use_reading)) }
                        }
                    }
                }
            }

            // Close affordance, always available.
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.statusBarsPadding().padding(DesignTokens.Spacing.s),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.tracking_action_close))
            }
        }
    }
}

/** Shows the captured photo with a content panel (spinner or OCR confirm) beneath it. */
@Composable
private fun CapturedPreview(
    uri: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(Res.string.tracking_capture_cd_photo),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(DesignTokens.Shape.roundedMd)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(DesignTokens.Spacing.xl))
        content()
    }
}
