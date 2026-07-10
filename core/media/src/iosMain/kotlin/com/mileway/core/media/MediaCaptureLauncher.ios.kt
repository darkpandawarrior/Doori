package com.mileway.core.media

import androidx.compose.runtime.Composable
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

// ponytail: placeholder so core:media's iOS targets compile standalone in V25/V26 P26.AND (the
// contract went @Composable in P26.AND for the Android actual). The real
// UIImagePickerController/PHPicker launcher lands in V26 P-IOS.
@Composable
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit =
    {
        throw NotImplementedError("rememberMediaCaptureLauncher: iOS actual lands in V26 P-IOS")
    }
