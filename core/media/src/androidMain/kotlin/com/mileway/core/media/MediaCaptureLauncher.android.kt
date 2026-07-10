package com.mileway.core.media

import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

// ponytail: placeholder so core:media's Android target compiles standalone in V25. The real
// CameraX/gallery/document-scanner launcher lands in V26 P-AND.
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit =
    {
        throw NotImplementedError("rememberMediaCaptureLauncher: Android actual lands in V26 P-AND")
    }
