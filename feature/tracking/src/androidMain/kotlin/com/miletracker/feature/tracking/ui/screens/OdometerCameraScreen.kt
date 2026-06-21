package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.data.model.display.OdometerCaptureResult
import com.miletracker.core.data.model.display.OdometerPurpose
import com.miletracker.feature.media.ui.camera.CameraCaptureScreen
import com.miletracker.feature.tracking.ui.sheets.OdometerReadingConfirmSheet

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (capturedUri == null) {
                CameraCaptureScreen(
                    isOdometerMode = true,
                    onCaptured = { uri ->
                        capturedUri = uri
                        captureTimeMs = System.currentTimeMillis()
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
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    val uri = capturedUri
    if (showConfirmSheet && uri != null) {
        OdometerReadingConfirmSheet(
            capturedUri = uri,
            purpose = purpose,
            baseReading = existingReading,
            sessionDistanceKm = sessionDistanceKm,
            onUseReading = { reading, isManual ->
                onResult(
                    OdometerCaptureResult(
                        purpose = purpose,
                        imageUri = uri,
                        reading = reading,
                        isManual = isManual,
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
