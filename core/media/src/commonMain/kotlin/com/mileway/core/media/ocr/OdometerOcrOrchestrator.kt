package com.mileway.core.media.ocr

/**
 * Multi-frame OCR: orchestration entry point.
 *
 * The platform recognizer ([FrameTextRecognizer]) is injected — it only has to turn one already-
 * captured frame into raw text + a quality signal; all decision logic (accept/reject, vote,
 * consensus, regex fallback) lives here in pure commonMain via [FrameQualityAnalyzer] and
 * [OcrAggregator]. Single-frame capture is just the N=1 case of [recognizeFrames].
 *
 * V26 P26.CONV: the sole odometer OCR pipeline — moved here from `feature:tracking` so
 * `feature:tracking`, `feature:media`, and `feature:logging` can all reach it without a
 * feature-to-feature dependency (see [OdometerOcrService] for the ready-to-inject facade).
 */
class OdometerOcrOrchestrator<T>(
    private val recognizer: FrameTextRecognizer<T>,
) {
    /**
     * Recognize and aggregate [frames] (one image handle per captured frame). A single-element list
     * is the existing single-shot capture flow — same result shape, no special-casing needed.
     */
    suspend fun recognizeFrames(frames: List<T>): OcrAggregator.AggregateResult {
        if (frames.isEmpty()) return OcrAggregator.AggregateResult(null, 0f, 0, 0)

        val candidates =
            frames.map { frame ->
                val recognized = recognizer.recognize(frame)
                OcrAggregator.FrameCandidate(rawText = recognized.text, quality = recognized.metrics)
            }
        return OcrAggregator.aggregate(candidates)
    }
}

/** One recognizer call's outcome: raw text plus the quality signals needed to score the frame. */
data class RecognizedFrame(
    val text: String,
    val metrics: FrameQualityAnalyzer.FrameMetrics,
)

/**
 * Platform text recognizer boundary. `T` is the platform's own image handle — generic so this
 * interface stays commonMain-clean with zero platform imports. The actual recognition is the only
 * platform-specific part; everything downstream of [RecognizedFrame] is pure.
 */
interface FrameTextRecognizer<T> {
    suspend fun recognize(frame: T): RecognizedFrame
}
