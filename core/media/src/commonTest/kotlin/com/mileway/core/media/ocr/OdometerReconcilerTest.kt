package com.mileway.core.media.ocr

import com.mileway.core.media.model.OdometerReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OdometerReconcilerTest {
    @Test
    fun `all three sources agree - accepted`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(userReading = "48213", deviceOcrReading = "48213", aiOcrReading = "48213"),
            )
        assertEquals(OdometerReconciler.Verdict.Accepted(48213), verdict)
    }

    @Test
    fun `sources within tolerance are accepted despite a small digit slip`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(userReading = "48213", deviceOcrReading = "48214", aiOcrReading = "48212"),
            )
        assertIs<OdometerReconciler.Verdict.Accepted>(verdict)
        assertEquals(48213, verdict.reading)
    }

    @Test
    fun `only one source present is trivially accepted`() {
        val verdict = OdometerReconciler.reconcile(OdometerReading(deviceOcrReading = "48213"))
        assertEquals(OdometerReconciler.Verdict.Accepted(48213), verdict)
    }

    @Test
    fun `moderate disagreement surfaces a discrepancy for the user to arbitrate`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(userReading = "48200", deviceOcrReading = "48213", aiOcrReading = "48213"),
            )
        assertIs<OdometerReconciler.Verdict.Discrepancy>(verdict)
        assertEquals(48200, verdict.userReading)
        assertEquals(48213, verdict.deviceReading)
    }

    @Test
    fun `wildly disagreeing sources are rejected rather than left for the user to arbitrate`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(userReading = "48213", deviceOcrReading = "1", aiOcrReading = "999999"),
            )
        assertIs<OdometerReconciler.Verdict.Rejected>(verdict)
    }

    @Test
    fun `no candidates at all is rejected`() {
        val verdict = OdometerReconciler.reconcile(OdometerReading())
        assertIs<OdometerReconciler.Verdict.Rejected>(verdict)
    }

    @Test
    fun `explicit isRejected flag short-circuits straight to rejection`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(userReading = "48213", isRejected = true, rejectionReason = "blurry photo"),
            )
        assertEquals(OdometerReconciler.Verdict.Rejected("blurry photo"), verdict)
    }

    @Test
    fun `exhausted retakes force-accepts the user reading over a disagreeing OCR pass`() {
        val verdict =
            OdometerReconciler.reconcile(
                OdometerReading(
                    userReading = "48213",
                    deviceOcrReading = "1",
                    retakeCount = OdometerReconciler.MAX_RETAKES,
                ),
            )
        assertEquals(OdometerReconciler.Verdict.Accepted(48213), verdict)
    }

    @Test
    fun `withRetake pushes the previous url into history and bumps the counter`() {
        val original = OdometerReading(url = "content://first", userReading = "100")
        val retaken = OdometerReconciler.withRetake(original, newUrl = "content://second")

        assertEquals("content://second", retaken.url)
        assertEquals(1, retaken.retakeCount)
        assertEquals(listOf("content://first"), retaken.retakeHistory)
    }

    @Test
    fun `withRetake accumulates history across multiple retakes`() {
        var reading = OdometerReading(url = "content://a")
        reading = OdometerReconciler.withRetake(reading, newUrl = "content://b")
        reading = OdometerReconciler.withRetake(reading, newUrl = "content://c")

        assertEquals(2, reading.retakeCount)
        assertEquals(listOf("content://a", "content://b"), reading.retakeHistory)
        assertTrue(reading.url == "content://c")
    }
}
