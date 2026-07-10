package com.mileway.core.media

import androidx.compose.runtime.Composable
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult

// ponytail: placeholder so core:media's desktop target compiles standalone in V25/V26 P26.AND (the
// contract went @Composable in P26.AND for the Android actual). Desktop capture (if ever needed —
// file-picker only, no camera) is out of scope until a plan schedules it. onOcrAnalysis (P26.SHEET)
// is unused here for the same reason — no capture, nothing to analyze.
@Composable
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
    onOcrAnalysis: (DocumentAnalysis) -> Unit,
): () -> Unit =
    {
        throw NotImplementedError("rememberMediaCaptureLauncher: desktop actual not yet scheduled")
    }
