package com.mileway.core.media.ocr

import com.mileway.core.ai.DuplicateDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptDedupHasherTest {
    private fun receipt(
        width: Int = 9,
        height: Int = 8,
    ): IntArray =
        IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (y < height / 2) 40 + x * 8 else 160 - x * 8
        }

    @Test
    fun `identical receipt has distance zero`() {
        val first = ReceiptDedupHasher.hash(receipt())
        assertEquals(0, DuplicateDetector.hammingDistance(first, ReceiptDedupHasher.hash(receipt())))
    }

    @Test
    fun `brightness shifted and slightly rescaled receipt stay inside threshold`() {
        val first = ReceiptDedupHasher.hash(receipt())
        val shifted = ReceiptDedupHasher.hash(receipt().map { it + 20 }.toIntArray())
        val rescaled = ReceiptDedupHasher.hash(receipt(11, 10), 11, 10)
        assertTrue(DuplicateDetector.hammingDistance(first, shifted) <= DuplicateDetector.IMAGE_MATCH_THRESHOLD)
        assertTrue(DuplicateDetector.hammingDistance(first, rescaled) <= DuplicateDetector.IMAGE_MATCH_THRESHOLD)
    }

    @Test
    fun `different receipt exceeds threshold`() {
        val first = ReceiptDedupHasher.hash(receipt())
        val different = ReceiptDedupHasher.hash(receipt().map { 255 - it }.toIntArray())
        assertTrue(DuplicateDetector.hammingDistance(first, different) > DuplicateDetector.IMAGE_MATCH_THRESHOLD)
    }
}
