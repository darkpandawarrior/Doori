package com.mileway.core.media.ocr

import androidx.compose.runtime.Composable
import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.TextRecognizer
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentImageRef

/**
 * Wraps `core:ai`'s [TextRecognizer] (the real ML Kit/Vision actuals `core:ai` already ships) as a
 * [FrameTextRecognizer] — portable across every platform with zero new expect/actual boundaries.
 * Quality is proxied from recognized-text line density, the same heuristic the old
 * `feature:tracking`-only `MlKitFrameTextRecognizer` used, generalized off plain text so it works for
 * both ML Kit's block/line structure (Android) and Vision's joined observations (iOS) alike.
 */
class DocumentIntelligenceFrameRecognizer(
    private val textRecognizer: TextRecognizer,
) : FrameTextRecognizer<DocumentImageRef> {
    override suspend fun recognize(frame: DocumentImageRef): RecognizedFrame {
        val text = textRecognizer.recognize(frame)
        return RecognizedFrame(text = text, metrics = text.toDensityMetrics())
    }
}

/**
 * ponytail: density-based proxy (no per-platform sharpness/contrast signal available from plain
 * text) — swap for real pixel-level sharpness/contrast if false-accepts show up in the field.
 */
private fun String.toDensityMetrics(): FrameQualityAnalyzer.FrameMetrics {
    val lines = lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return FrameQualityAnalyzer.FrameMetrics(0f, 0f, 0f)
    val densityScore = (lines.map { it.length }.average().toFloat() / 12f).coerceIn(0f, 1f)
    return FrameQualityAnalyzer.FrameMetrics(sharpness = densityScore, contrast = densityScore, textConfidence = densityScore)
}

/**
 * Gallery-image multi-pass odometer recognition boundary (V26 P26.CONV.1) — carries forward
 * `feature:media`'s retired `GalleryOdometerProcessor`/`OdometerOcrAggregator` multi-pass-
 * verification idea (re-run recognition over several image-enhancement variants of the same picked
 * photo, then vote) onto the one shared [OcrAggregator]. Android's actual does the real multi-pass
 * work; platforms without an image-enhancement pipeline yet fall back to [SinglePassGalleryRecognizer].
 */
interface GalleryMultiPassRecognizer {
    suspend fun recognize(image: DocumentImageRef): OcrAggregator.AggregateResult
}

/** Single-pass fallback: N=1 case of the same aggregator, used where no multi-pass actual exists yet. */
class SinglePassGalleryRecognizer(
    textRecognizer: TextRecognizer,
) : GalleryMultiPassRecognizer {
    private val orchestrator = OdometerOcrOrchestrator(DocumentIntelligenceFrameRecognizer(textRecognizer))

    override suspend fun recognize(image: DocumentImageRef): OcrAggregator.AggregateResult = orchestrator.recognizeFrames(listOf(image))
}

private val ODOMETER_PROMPT =
    DocPrompt(
        docType = DocType.ODOMETER,
        instruction = "Read the vehicle odometer value shown in this photo as a plain integer.",
        schemaHint = "integer distance reading, digits only",
    )

/**
 * V26 P26.CONV: the one public odometer-OCR entry point every caller (camera capture, gallery pick,
 * a manually-attached photo) uses — replaces the three previously-competing pipelines
 * (`feature:tracking`'s `OdometerOcrOrchestrator`, `feature:media`'s `GalleryOdometerProcessor`,
 * `feature:logging`'s manual-only `OdometerCaptureSheet` path).
 */
class OdometerOcrService(
    textRecognizer: TextRecognizer,
    private val galleryRecognizer: GalleryMultiPassRecognizer,
    private val documentIntelligence: DocumentIntelligence,
) {
    private val orchestrator = OdometerOcrOrchestrator(DocumentIntelligenceFrameRecognizer(textRecognizer))

    /** Single-shot capture (camera photo, or an already-picked photo) — the N=1 case of [analyzeFrames]. */
    suspend fun analyzeSingle(image: DocumentImageRef): OcrAggregator.AggregateResult = orchestrator.recognizeFrames(listOf(image))

    /** Multi-frame burst capture: N frames aggregated into one consensus reading. */
    suspend fun analyzeFrames(images: List<DocumentImageRef>): OcrAggregator.AggregateResult = orchestrator.recognizeFrames(images)

    /** Gallery-picked image: platform multi-pass verification (Android) or single pass (elsewhere). */
    suspend fun analyzeGalleryImage(image: DocumentImageRef): OcrAggregator.AggregateResult = galleryRecognizer.recognize(image)

    /**
     * V26 P26.CONV.3: the AI-tier reading for 3-way reconciliation ([OdometerReconciler]), sourced
     * from `core:ai`'s [DocumentIntelligence] rather than the plain OCR pass above. Null when the
     * on-device AI analyzer isn't available on this hardware or didn't extract an odometer field —
     * [DocumentIntelligence] never throws, so this never needs to either.
     */
    suspend fun analyzeAi(image: DocumentImageRef): String? = documentIntelligence.analyze(image, ODOMETER_PROMPT).fields[DocField.ODOMETER]?.value
}

/** Builds the platform [OdometerOcrService] (real ML Kit on Android, real Vision on iOS). */
@Composable
expect fun rememberOdometerOcrService(): OdometerOcrService
