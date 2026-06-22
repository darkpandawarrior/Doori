package com.miletracker

import com.miletracker.feature.media.ocr.OdometerOcrAggregator
import com.miletracker.feature.media.ocr.OdometerOcrAggregator.PassResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D.2: proves the pure multi-pass OCR aggregation — majority agreement, agreement-based confidence with a
 * labelled-line bonus, the >=2-passes `isVerified` gate, and deterministic tie-breaking.
 */
class OdometerOcrAggregatorTest {
    @Test
    fun `majority reading wins, is verified, and gets the labelled bonus`() {
        val aggregate =
            OdometerOcrAggregator.aggregate(
                listOf(
                    PassResult("default", "048213", labelled = false),
                    PassResult("high_contrast", "048213", labelled = true),
                    PassResult("grayscale", "048213", labelled = false),
                    PassResult("brighten", "048218", labelled = false),
                ),
            )
        assertEquals("048213", aggregate.reading)
        assertEquals(3, aggregate.agreeingPasses)
        assertEquals(4, aggregate.totalPasses)
        assertTrue(aggregate.isVerified)
        // 3/4 agreement (0.75) + labelled bonus (0.10) = 0.85
        assertEquals(0.85f, aggregate.confidence, 0.0001f)
    }

    @Test
    fun `a single agreeing pass is not verified`() {
        val aggregate =
            OdometerOcrAggregator.aggregate(
                listOf(
                    PassResult("default", "12345", labelled = false),
                    PassResult("high_contrast", null, labelled = false),
                    PassResult("grayscale", null, labelled = false),
                    PassResult("brighten", null, labelled = false),
                ),
            )
        assertEquals("12345", aggregate.reading)
        assertEquals(1, aggregate.agreeingPasses)
        assertFalse(aggregate.isVerified)
        assertEquals(0.25f, aggregate.confidence, 0.0001f) // 1/4, no labelled bonus
    }

    @Test
    fun `no readings yields null with zero confidence`() {
        val aggregate =
            OdometerOcrAggregator.aggregate(
                listOf(
                    PassResult("default", null, labelled = false),
                    PassResult("brighten", null, labelled = false),
                ),
            )
        assertNull(aggregate.reading)
        assertEquals(0f, aggregate.confidence, 0.0001f)
        assertFalse(aggregate.isVerified)
    }

    @Test
    fun `ties break toward the longer, more specific reading`() {
        val aggregate =
            OdometerOcrAggregator.aggregate(
                listOf(
                    PassResult("default", "4821", labelled = false),
                    PassResult("brighten", "048213", labelled = false),
                ),
            )
        assertEquals("048213", aggregate.reading)
        assertEquals(1, aggregate.agreeingPasses)
        assertFalse(aggregate.isVerified)
    }
}
