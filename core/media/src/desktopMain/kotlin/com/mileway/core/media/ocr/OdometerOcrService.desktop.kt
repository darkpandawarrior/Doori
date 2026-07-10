package com.mileway.core.media.ocr

import androidx.compose.runtime.Composable

// ponytail: placeholder so core:media's desktop target compiles standalone, matching
// MediaCaptureLauncher.desktop.kt's convention. Desktop odometer capture is out of scope until a
// plan schedules it.
@Composable
actual fun rememberOdometerOcrService(): OdometerOcrService {
    throw NotImplementedError("rememberOdometerOcrService: desktop actual not yet scheduled")
}
