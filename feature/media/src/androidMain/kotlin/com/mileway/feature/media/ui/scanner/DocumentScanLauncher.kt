package com.mileway.feature.media.ui.scanner

import androidx.compose.runtime.Composable
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher

/** Uses the Play scanner or the system document picker in FOSS builds. */
@Composable
fun rememberDocumentScanLauncher(
    pageLimit: Int = 5,
    onScanned: (List<String>) -> Unit,
): () -> Unit =
    rememberMediaCaptureLauncher(
        config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Document), multiple = true, maxCount = pageLimit),
        onResult = { result ->
            if (result is MediaCaptureResult.Attachments) onScanned(result.items.map { it.uri })
        },
    )
