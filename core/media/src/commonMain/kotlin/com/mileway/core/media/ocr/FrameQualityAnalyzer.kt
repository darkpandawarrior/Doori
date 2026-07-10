package com.mileway.core.media.ocr

/**
 * Multi-frame OCR: pure quality gate for a single camera frame.
 *
 * A frame is worth feeding to the aggregator only if the platform recognizer had a reasonable shot
 * at it. No Android/ML Kit/Vision types here — [sharpness] and [contrast] are cheap proxies the
 * platform actual can compute from its own image buffer (e.g. Laplacian variance, histogram spread)
 * and [textConfidence] is the recognizer's own per-block/line confidence average. Pure Kotlin so
 * it's directly JVM-unit-testable.
 *
 * V26 P26.CONV: moved here from `feature:tracking` (single sole odometer OCR pipeline, reachable by
 * every feature module without a feature-to-feature dependency).
 */
object FrameQualityAnalyzer {
    /** Per-frame inputs, each normalised to 0f..1f by the caller (platform actual). */
    data class FrameMetrics(
        val sharpness: Float,
        val contrast: Float,
        val textConfidence: Float,
    )

    // ponytail: flat weighted average — swap for a learned model only if false-accepts show up in the field.
    private const val SHARPNESS_WEIGHT = 0.4f
    private const val CONTRAST_WEIGHT = 0.2f
    private const val CONFIDENCE_WEIGHT = 0.4f

    /** Below this score a frame is too poor to vote in the aggregate. */
    const val ACCEPT_THRESHOLD = 0.35f

    /** Weighted quality score in 0f..1f. */
    fun score(metrics: FrameMetrics): Float =
        (
            metrics.sharpness.coerceIn(0f, 1f) * SHARPNESS_WEIGHT +
                metrics.contrast.coerceIn(0f, 1f) * CONTRAST_WEIGHT +
                metrics.textConfidence.coerceIn(0f, 1f) * CONFIDENCE_WEIGHT
        ).coerceIn(0f, 1f)

    /** Convenience gate: should this frame's candidate reading be trusted at all? */
    fun isAcceptable(metrics: FrameMetrics): Boolean = score(metrics) >= ACCEPT_THRESHOLD
}
