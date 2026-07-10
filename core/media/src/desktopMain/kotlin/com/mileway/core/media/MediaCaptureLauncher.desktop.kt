package com.mileway.core.media

import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

// ponytail: placeholder so core:media's desktop target compiles standalone in V25. Desktop capture
// (if ever needed — file-picker only, no camera) is out of scope until a plan schedules it.
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit =
    {
        throw NotImplementedError("rememberMediaCaptureLauncher: desktop actual not yet scheduled")
    }
