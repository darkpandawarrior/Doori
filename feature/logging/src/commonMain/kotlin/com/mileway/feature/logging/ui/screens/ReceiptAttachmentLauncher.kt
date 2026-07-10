package com.mileway.feature.logging.ui.screens

import androidx.compose.runtime.Composable
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher

/**
 * Opens the platform's photo picker for an optional local receipt attachment (P1.4). Returns a
 * callback that launches the picker; [onPicked] is invoked once per picked image's local URI/path
 * string (never invoked on cancel). Callers that track a single receipt field (all of today's do)
 * naturally keep the last invocation — the same "one photo" outcome as before multi-select existed.
 *
 * V26 P26.SITE.2: thin wrapper over `core:media`'s [rememberMediaCaptureLauncher]
 * ([CaptureMode.Gallery] — the one mode with a real actual on both Android and iOS) instead of a
 * hand-rolled Android-only `ActivityResultContracts.PickVisualMedia` plus a truthful iOS no-op.
 * Gains: real iOS support, multi-select (`config.multiple`), and OCR — [MediaCaptureConfig.enableOcr]
 * runs the picked image(s) through `core:ai`'s `DocumentIntelligence` and shows the matching
 * `OcrReviewSheet`/`OcrBatchResultsSheet` (defaults to [DocType.RECEIPT]) before [onPicked] fires.
 */
@Composable
fun rememberReceiptAttachmentLauncher(onPicked: (String) -> Unit): () -> Unit =
    rememberMediaCaptureLauncher(
        config =
            MediaCaptureConfig(
                allowedModes = setOf(CaptureMode.Gallery),
                multiple = true,
                maxCount = 5,
                enableOcr = true,
            ),
        onResult = { result ->
            if (result is MediaCaptureResult.Attachments) {
                result.items.forEach { onPicked(it.uri) }
            }
        },
    )
