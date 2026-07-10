package com.mileway.core.media

import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

// ponytail: placeholder so core:media's iOS targets compile standalone in V25. The real
// UIImagePickerController/PHPicker launcher lands in V26 P-IOS.
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit =
    {
        throw NotImplementedError("rememberMediaCaptureLauncher: iOS actual lands in V26 P-IOS")
    }
