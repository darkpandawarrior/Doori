package com.mileway.core.media.watermark

// ponytail: desktop capture itself is unimplemented (MediaCaptureLauncher.desktop.kt throws
// NotImplementedError for every mode), so nothing ever reaches this call in practice. Returning
// the uri unchanged — rather than throwing — keeps this actual harmless if that ever changes
// before desktop capture is scheduled.
actual suspend fun burnWatermark(
    imageUri: String,
    text: String,
): String = imageUri
