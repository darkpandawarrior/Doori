package com.mileway.core.media.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization test for the V26 P26.CONV move from `feature:tracking` into `core:media`: every
 * assertion here pins the pre-move numeric behavior (consensus reading, quality-weighted voting,
 * regex-fallback bounds) so the move + the additive `labelled`-bonus change can't silently drift it.
 */
class OcrAggregatorTest {
    private fun frame(
        text: String,
        quality: Float = 0.9f,
        labelled: Boolean = false,
    ) = OcrAggregator.FrameCandidate(
        rawText = text,
        quality = FrameQualityAnalyzer.FrameMetrics(quality, quality, quality),
        labelled = labelled,
    )

    @Test
    fun `single frame path returns its own reading`() {
        val result = OcrAggregator.aggregate(listOf(frame("ODO 48213")))
        assertEquals(48213, result.reading)
        assertEquals(1, result.agreeingFrames)
        assertEquals(1, result.totalFrames)
    }

    @Test
    fun `agreeing frames converge on the consensus reading`() {
        val result =
            OcrAggregator.aggregate(
                listOf(frame("48213 km"), frame("reading 48213"), frame("48213")),
            )
        assertEquals(48213, result.reading)
        assertEquals(3, result.agreeingFrames)
        assertTrue(result.isVerified)
    }

    @Test
    fun `disagreement is broken by quality-weighted vote`() {
        // Two low-quality frames vs one high-quality frame — the sharp frame's group wins.
        val result =
            OcrAggregator.aggregate(
                listOf(
                    frame("11111", quality = 0.1f),
                    frame("11111", quality = 0.1f),
                    frame("22222", quality = 0.95f),
                ),
            )
        assertEquals(22222, result.reading)
    }

    @Test
    fun `numeric tie-break prefers the larger reading`() {
        val result =
            OcrAggregator.aggregate(
                listOf(frame("11111", quality = 0.5f), frame("22222", quality = 0.5f)),
            )
        assertEquals(22222, result.reading)
    }

    @Test
    fun `low-quality frames are rejected before voting`() {
        // Two very poor frames disagree with one good frame; poor ones should be dropped entirely,
        // leaving the good frame's reading as the sole survivor.
        val result =
            OcrAggregator.aggregate(
                listOf(
                    frame("99999", quality = 0.05f),
                    frame("88888", quality = 0.05f),
                    frame("48213", quality = 0.9f),
                ),
            )
        assertEquals(48213, result.reading)
        assertEquals(1, result.agreeingFrames)
    }

    @Test
    fun `regex fallback extracts a reading from noisy text`() {
        val reading = OcrAggregator.extractReading("0d0meter: 04821O km")
        assertEquals(48210, reading)
    }

    @Test
    fun `out-of-bounds reading is rejected`() {
        // 7-digit 1234567 exceeds the 999_999 ceiling; the 3-digit 999 is below the 4-digit regex
        // floor. Nothing survives — extractor is bounds-only (date/price filtering is not its job).
        assertNull(OcrAggregator.extractReading("id 999 code 1234567"))
    }

    @Test
    fun `all frames rejected as low quality still surfaces a best-effort reading`() {
        // Every frame is poor quality — falls back to voting over the full (unfiltered) pool rather
        // than reporting nothing, so the user still gets something to manually confirm.
        val result = OcrAggregator.aggregate(listOf(frame("48213", quality = 0.01f)))
        assertEquals(48213, result.reading)
    }

    @Test
    fun `empty frame list returns no reading`() {
        val result = OcrAggregator.aggregate(emptyList())
        assertNull(result.reading)
        assertEquals(0, result.totalFrames)
    }

    @Test
    fun `default labelled=false keeps confidence identical to the pre-move formula`() {
        // Pins the exact pre-move confidence value: agreementRatio=1, avgQuality=0.9,
        // confidence = (1 + 0.9) / 2 = 0.95, no labelled bonus.
        val result = OcrAggregator.aggregate(listOf(frame("48213", quality = 0.9f)))
        assertTrue(kotlin.math.abs(result.confidence - 0.95f) < 0.0001f, "expected ~0.95, got ${result.confidence}")
    }

    @Test
    fun `labelled candidate adds a confidence bonus without changing the winning reading`() {
        val unlabelled = OcrAggregator.aggregate(listOf(frame("48213", quality = 0.9f)))
        val labelled = OcrAggregator.aggregate(listOf(frame("48213", quality = 0.9f, labelled = true)))

        assertEquals(unlabelled.reading, labelled.reading)
        assertTrue(labelled.confidence > unlabelled.confidence)
    }
}
