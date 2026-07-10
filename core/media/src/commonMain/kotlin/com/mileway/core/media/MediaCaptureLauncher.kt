package com.mileway.core.media

import androidx.compose.runtime.Composable
import com.mileway.core.ai.model.DocumentAnalysis
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
 *
 * V26 P26.SHEET: when [MediaCaptureConfig.enableOcr] is set, the Android actual runs
 * `core:ai`'s `DocumentIntelligence` on the captured attachment(s) and shows the matching
 * `core:ui` sheet (`OcrResultHost`/`OcrBatchResultsSheet`) before calling [onResult] — the
 * unified-contract payoff every `enableOcr` caller gets automatically, no extra plumbing beyond
 * setting the flag. [onOcrAnalysis] is an optional hook for a caller that wants the extracted
 * fields (e.g. to autofill an expense form) once the user taps "Use data"; it is a no-op when
 * OCR isn't enabled/run. iOS/desktop actuals don't run analysis yet (see their bodies) — the
 * param still exists there so every platform shares one contract.
 */
@Composable
expect fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
    onOcrAnalysis: (DocumentAnalysis) -> Unit = {},
): () -> Unit
