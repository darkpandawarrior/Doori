package com.mileway.core.media

import androidx.compose.runtime.Composable
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

/**
 * Prepares a media-capture flow for [config] and delivers its outcome to [onResult]. Returns a
 * trigger function — call it to start the flow.
 *
 * V25 P25.A1.2 declared this signature as a plain function so `core:media` stayed Compose-free
 * until a real actual needed the composition-aware launcher APIs. V26 P26.AND's Android actual
 * needs exactly that — `rememberLauncherForActivityResult` and Peekaboo's
 * `rememberImagePickerLauncher` are both `@Composable` — so the contract becomes `@Composable`
 * here; `core:media` now takes the Compose Multiplatform plugin (`shared.kmp.compose`).
 */
@Composable
expect fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit
