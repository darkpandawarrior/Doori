package com.mileway.core.media.ocr

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrameQualityAnalyzerTest {
    private fun metrics(
        sharpness: Float,
        contrast: Float,
        textConfidence: Float,
    ) = FrameQualityAnalyzer.FrameMetrics(sharpness, contrast, textConfidence)

    @Test
    fun `sharp high-confidence frame is acceptable`() {
        assertTrue(FrameQualityAnalyzer.isAcceptable(metrics(0.9f, 0.9f, 0.9f)))
    }

    @Test
    fun `blurry low-confidence frame is rejected`() {
        assertFalse(FrameQualityAnalyzer.isAcceptable(metrics(0.1f, 0.1f, 0.1f)))
    }

    @Test
    fun `score is clamped into 0f to 1f even with out-of-range inputs`() {
        val score = FrameQualityAnalyzer.score(metrics(2f, -1f, 5f))
        assertTrue(score in 0f..1f, "expected clamped score, got $score")
    }

    @Test
    fun `borderline frame right at threshold is acceptable`() {
        // Equal weighting of all three inputs at the threshold value should pass the >= gate.
        val atThreshold = FrameQualityAnalyzer.ACCEPT_THRESHOLD
        assertTrue(FrameQualityAnalyzer.isAcceptable(metrics(atThreshold, atThreshold, atThreshold)))
    }
}
