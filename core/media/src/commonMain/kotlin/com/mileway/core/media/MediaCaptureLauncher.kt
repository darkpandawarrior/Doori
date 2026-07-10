package com.mileway.core.media

import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

/**
 * Prepares a media-capture flow for [config] and delivers its outcome to [onResult]. Returns a
 * trigger function — call it to start the flow.
 *
 * V25 P25.A1.2: signature only, establishing the stable contract V26/V27/V29/V30 build against.
 * Deliberately a plain function (not `@Composable`) so `core:media` stays Compose-free until a real
 * platform actual needs the composition-aware launcher APIs (`rememberLauncherForActivityResult` on
 * Android, `UIImagePickerController` bridging on iOS) in V26.
 */
expect fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit
