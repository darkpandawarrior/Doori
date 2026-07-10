package com.mileway.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.ocr.rememberOdometerOcrService
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_odometer_cd_pick_gallery
import com.mileway.feature.media.ui.camera.CameraCaptureScreen
import com.mileway.feature.tracking.ui.sheets.OdometerReadingConfirmSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen odometer capture flow. Shows the live camera with the odometer alignment
 * overlay; after the user captures a photo, shows [OdometerReadingConfirmSheet] for
 * simulated-OCR confirmation. Result is delivered via [onResult]; [onBack] dismisses.
 *
 * @param purpose       Whether capturing the start or end odometer reading.
 * @param existingReading Pre-fill base reading for the confirm sheet (START baseline).
 * @param sessionDistanceKm GPS distance, used to estimate the END reading.
 * @param onResult      Called with the confirmed capture result.
 * @param onBack        Called when the user taps the back arrow without confirming.
 */
@Composable
fun OdometerCameraScreen(
    purpose: OdometerPurpose,
    existingReading: Int = 45_000,
    sessionDistanceKm: Double = 0.0,
    onResult: (OdometerCaptureResult) -> Unit,
    onBack: () -> Unit,
) {
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var showConfirmSheet by remember { mutableStateOf(false) }
    var captureTimeMs by remember { mutableStateOf(0L) }
    // D.2b: multi-pass OCR reading parsed from a gallery-picked image; pre-fills the confirm sheet.
    var galleryReading by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()
    // V26 P26.CONV.1: the one shared odometer OCR pipeline's gallery entry point — real multi-pass
    // (image-enhancement variants + voting) on Android, carrying forward the retired
    // feature:media GalleryOdometerProcessor's multi-pass-verification idea.
    val ocrService = rememberOdometerOcrService()
    // V26 P26.SITE.1: routed through core:media's unified launcher (CaptureMode.Gallery) instead
    // of a hand-rolled PickVisualMedia contract — the odometer confirm/discrepancy/rejection UX
    // (P26.CONV) and the multi-pass gallery OCR pipeline are unchanged, only the picker itself
    // moved. enableOcr stays false: the generic OcrReviewSheet isn't this screen's odometer UX,
    // ocrService.analyzeGalleryImage below already is.
    val galleryLauncher =
        rememberMediaCaptureLauncher(
            config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Gallery)),
            onResult = { result ->
                val uriStr =
                    (result as? MediaCaptureResult.Attachments)?.items?.firstOrNull()?.uri
                        ?: return@rememberMediaCaptureLauncher
                capturedUri = uriStr
                captureTimeMs = System.currentTimeMillis()
                scope.launch {
                    galleryReading = ocrService.analyzeGalleryImage(uriStr).reading
                    showConfirmSheet = true
                }
            },
        )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (capturedUri == null) {
                CameraCaptureScreen(
                    isOdometerMode = true,
                    onCaptured = { uri ->
                        capturedUri = uri
                        captureTimeMs = System.currentTimeMillis()
                        galleryReading = null
                        showConfirmSheet = true
                    },
                )
            } else {
                // Photo captured, leave camera view in place; confirm sheet renders above.
                CameraCaptureScreen(
                    isOdometerMode = true,
                    onCaptured = {},
                )
            }

            // Back arrow (always visible over the camera)
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.tracking_cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // D.2b: pick an existing odometer photo from the gallery instead of capturing.
            if (capturedUri == null) {
                IconButton(
                    onClick = galleryLauncher,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = stringResource(Res.string.tracking_odometer_cd_pick_gallery),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    val uri = capturedUri
    if (showConfirmSheet && uri != null) {
        OdometerReadingConfirmSheet(
            capturedUri = uri,
            purpose = purpose,
            // D.2b: when the image came from the gallery, seed the confirm sheet with its multi-pass OCR reading.
            baseReading = galleryReading ?: existingReading,
            sessionDistanceKm = sessionDistanceKm,
            onUseReading = { reading, isManual ->
                onResult(
                    OdometerCaptureResult(
                        purpose = purpose,
                        imageUri = uri,
                        reading = reading,
                        source = if (isManual) OdometerReadingSource.MANUAL else OdometerReadingSource.DEVICE_OCR,
                        captureTimeMs = captureTimeMs,
                    ),
                )
            },
            onRetake = {
                capturedUri = null
                showConfirmSheet = false
            },
            onDismiss = {
                showConfirmSheet = false
            },
        )
    }
}
