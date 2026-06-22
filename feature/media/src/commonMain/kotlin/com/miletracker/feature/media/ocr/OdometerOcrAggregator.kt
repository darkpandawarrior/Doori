package com.miletracker.feature.media.ocr

/**
 * D.2: pure aggregator for **multi-pass** odometer OCR.
 *
 * A single ML Kit pass on a raw photo is fragile (glare, low light, motion blur). The production path
 * now runs the recogniser over several image-enhancement variants of the same frame (default,
 * high-contrast, grayscale, brightened) and feeds each pass's parsed reading here. The reading that the
 * most passes agree on wins; confidence is the agreement ratio plus a small bonus when an agreeing pass
 * found the number on a *labelled* line ("ODO 48213"). `isVerified` means ≥2 passes independently agreed.
 *
 * Pure Kotlin (no Android / ML Kit) so it is directly JVM-unit-testable, like [OdometerOcrParser].
 */
object OdometerOcrAggregator {
    /** One enhancement pass's outcome: the parsed reading (or null) and whether it was a labelled hit. */
    data class PassResult(
        val variant: String,
        val reading: String?,
        val labelled: Boolean,
    )

    data class Aggregate(
        val reading: String?,
        val confidence: Float,
        val agreeingPasses: Int,
        val totalPasses: Int,
    ) {
        /** A reading is trustworthy once at least two independent passes agree on it. */
        val isVerified: Boolean get() = reading != null && agreeingPasses >= MIN_AGREE
    }

    private const val MIN_AGREE = 2
    private const val LABELLED_BONUS = 0.1f

    /**
     * Fold per-pass readings into a single agreed reading + confidence. Ties are broken toward the
     * longer (more specific) reading, then the numerically larger one, for determinism.
     */
    fun aggregate(passes: List<PassResult>): Aggregate {
        val total = passes.size
        val withReading = passes.filter { it.reading != null }
        if (withReading.isEmpty()) return Aggregate(null, 0f, 0, total)

        val byReading = withReading.groupBy { it.reading!! }
        val winner =
            byReading.entries.maxWith(
                compareBy<Map.Entry<String, List<PassResult>>> { it.value.size }
                    .thenBy { it.key.length }
                    .thenBy { it.key.toLongOrNull() ?: 0L },
            )
        val reading = winner.key
        val agree = winner.value.size
        val agreementRatio = if (total == 0) 0f else agree.toFloat() / total
        val labelledBonus = if (winner.value.any { it.labelled }) LABELLED_BONUS else 0f
        val confidence = (agreementRatio + labelledBonus).coerceIn(0f, 1f)
        return Aggregate(reading, confidence, agree, total)
    }
}
