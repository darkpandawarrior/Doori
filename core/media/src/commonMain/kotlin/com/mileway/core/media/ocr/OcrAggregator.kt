package com.mileway.core.media.ocr

/**
 * Multi-frame OCR: pure aggregator + regex-fallback extractor.
 *
 * Consumes already-recognized text for N frames (the platform recognizer is a thin actual — see
 * [OdometerOcrOrchestrator]) and produces a single consensus odometer reading. No Android/ML Kit
 * imports, directly JVM-unit-testable.
 *
 * Consensus rule: each frame's raw text is preprocessed and regex-extracted into a candidate
 * reading; frames below [FrameQualityAnalyzer.ACCEPT_THRESHOLD] are dropped before voting. Surviving
 * candidates are grouped by reading and the group with the highest **summed quality** wins (a
 * quality-weighted vote, not a plain majority) — one sharp frame should be able to outvote two blurry
 * ones. Ties are broken by (1) more agreeing frames, then (2) numerically larger reading, for
 * determinism. A single frame is the degenerate N=1 case: it always "wins" its own group.
 *
 * V26 P26.CONV: moved here from `feature:tracking` to become the one shared pipeline reachable by
 * every feature. [FrameCandidate.labelled] carries over feature:media's `GalleryOdometerProcessor`/
 * `OdometerOcrAggregator` multi-pass insight — a candidate found on a line that also carries an
 * odometer label ("ODO 48213") is a stronger signal — as a small additive confidence bonus. Default
 * `labelled = false` on every existing call site keeps the pre-move numeric behavior byte-for-byte
 * identical (see [OcrAggregatorTest], moved unchanged as the characterization test for this move).
 */
object OcrAggregator {
    private val DIGIT_GROUP = Regex("\\d{4,7}")
    private val bounds = 1_000..999_999
    private const val LABELLED_BONUS = 0.1f

    data class FrameCandidate(
        val rawText: String,
        val quality: FrameQualityAnalyzer.FrameMetrics,
        val labelled: Boolean = false,
    )

    data class AggregateResult(
        val reading: Int?,
        val confidence: Float,
        val agreeingFrames: Int,
        val totalFrames: Int,
    ) {
        val isVerified: Boolean get() = reading != null && agreeingFrames >= 1
    }

    /** Strip common OCR misreads before regex extraction: O/o->0, I/l->1, S/s->5, B/b->8 near digits. */
    fun preprocess(text: String): String =
        text
            .replace(Regex("(?<=\\d)[Oo]|[Oo](?=\\d)"), "0")
            .replace(Regex("(?<=\\d)[Il]|[Il](?=\\d)"), "1")
            .replace(Regex("(?<=\\d)[Ss]|[Ss](?=\\d)"), "5")
            .replace(Regex("(?<=\\d)[Bb]|[Bb](?=\\d)"), "8")

    /** Regex-fallback extractor: longest in-bounds 4-7 digit run, matching the legacy single-shot rule. */
    fun extractReading(rawText: String): Int? =
        DIGIT_GROUP.findAll(preprocess(rawText))
            .mapNotNull { it.value.toIntOrNull() }
            .filter { it in bounds }
            .maxOrNull()

    /**
     * Vote across frames. Quality-rejected frames (score < threshold) are excluded entirely; if that
     * empties the set, falls back to voting over all frames rather than reporting no result — a
     * genuinely poor batch should still surface its best guess to the user for manual confirmation.
     */
    fun aggregate(frames: List<FrameCandidate>): AggregateResult {
        val total = frames.size
        if (frames.isEmpty()) return AggregateResult(null, 0f, 0, 0)

        val accepted = frames.filter { FrameQualityAnalyzer.isAcceptable(it.quality) }
        val pool = accepted.ifEmpty { frames }

        val withReading =
            pool.mapNotNull { frame ->
                extractReading(frame.rawText)?.let { Triple(it, frame.quality, frame.labelled) }
            }
        if (withReading.isEmpty()) return AggregateResult(null, 0f, 0, total)

        val byReading = withReading.groupBy({ it.first }, { it.second to it.third })
        val winner =
            byReading.entries.maxWith(
                compareBy<Map.Entry<Int, List<Pair<FrameQualityAnalyzer.FrameMetrics, Boolean>>>> { entry ->
                    entry.value.sumOf { (quality, _) -> FrameQualityAnalyzer.score(quality).toDouble() }
                }
                    .thenBy { it.value.size }
                    .thenBy { it.key },
            )

        val agree = winner.value.size
        val avgQuality = winner.value.map { (quality, _) -> FrameQualityAnalyzer.score(quality) }.average().toFloat()
        val agreementRatio = agree.toFloat() / pool.size
        val labelledBonus = if (winner.value.any { (_, labelled) -> labelled }) LABELLED_BONUS else 0f
        val confidence = ((agreementRatio + avgQuality) / 2f + labelledBonus).coerceIn(0f, 1f)

        return AggregateResult(
            reading = winner.key,
            confidence = confidence,
            agreeingFrames = agree,
            totalFrames = total,
        )
    }
}
