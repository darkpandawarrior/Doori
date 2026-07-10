package com.mileway.core.ai

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DuplicateDetectorTest {
    private val detector = DuplicateDetector(windowMinutes = 5)
    private val source = AnalyzerSource.ON_DEVICE_AI

    private fun fields(
        merchant: String? = "Cafe Roma",
        total: String? = "12.50",
    ): Map<DocField, ExtractedValue> =
        buildMap {
            merchant?.let { put(DocField.MERCHANT, ExtractedValue(it, 0.9f, source)) }
            total?.let { put(DocField.TOTAL, ExtractedValue(it, 0.9f, source)) }
        }

    @Test
    fun `no candidates is Unique`() {
        val verdict = detector.check(fields(), timestampMillis = 0L, candidates = emptyList())

        assertEquals(DuplicateVerdict.Unique, verdict)
    }

    @Test
    fun `missing merchant is Unique even with a matching candidate`() {
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))

        val verdict = detector.check(fields(merchant = null), timestampMillis = 0L, candidates = candidates)

        assertEquals(DuplicateVerdict.Unique, verdict)
    }

    @Test
    fun `missing amount is Unique even with a matching candidate`() {
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))

        val verdict = detector.check(fields(total = null), timestampMillis = 0L, candidates = candidates)

        assertEquals(DuplicateVerdict.Unique, verdict)
    }

    @Test
    fun `different merchant is Unique`() {
        val candidates = listOf(DedupCandidate("ref-1", "Other Shop", "12.50", 0L))

        val verdict = detector.check(fields(), timestampMillis = 0L, candidates = candidates)

        assertEquals(DuplicateVerdict.Unique, verdict)
    }

    @Test
    fun `same merchant and amount at the exact same timestamp is Confirmed`() {
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 1_000L))

        val verdict = detector.check(fields(), timestampMillis = 1_000L, candidates = candidates)

        assertEquals(DuplicateVerdict.Confirmed("ref-1"), verdict)
    }

    @Test
    fun `same merchant and amount within the window but different time is Possible`() {
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))

        val verdict = detector.check(fields(), timestampMillis = 60_000L, candidates = candidates)

        assertIs<DuplicateVerdict.Possible>(verdict)
        assertEquals("ref-1", verdict.ref)
    }

    @Test
    fun `exactly N minutes apart is still within the window (inclusive boundary)`() {
        val fiveMinutesMillis = 5 * 60_000L
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))

        val verdict = detector.check(fields(), timestampMillis = fiveMinutesMillis, candidates = candidates)

        assertIs<DuplicateVerdict.Possible>(verdict)
    }

    @Test
    fun `one millisecond beyond the window is Unique`() {
        val justOverFiveMinutes = 5 * 60_000L + 1
        val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))

        val verdict = detector.check(fields(), timestampMillis = justOverFiveMinutes, candidates = candidates)

        assertEquals(DuplicateVerdict.Unique, verdict)
    }

    @Test
    fun `merchant comparison is case and whitespace insensitive`() {
        val candidates = listOf(DedupCandidate("ref-1", "  CAFE ROMA  ", "12.50", 0L))

        val verdict = detector.check(fields(), timestampMillis = 0L, candidates = candidates)

        assertEquals(DuplicateVerdict.Confirmed("ref-1"), verdict)
    }
}
