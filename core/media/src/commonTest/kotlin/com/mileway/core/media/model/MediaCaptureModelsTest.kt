package com.mileway.core.media.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCaptureModelsTest {
    @Test
    fun `MediaCaptureConfig has sensible defaults`() {
        val config = MediaCaptureConfig()

        assertEquals(setOf(CaptureMode.Camera), config.allowedModes)
        assertEquals(false, config.multiple)
        assertEquals(1, config.maxCount)
        assertEquals(false, config.enableOcr)
        assertEquals(false, config.isOdometer)
        assertEquals(true, config.allowManualInput)
        assertEquals(false, config.watermarkingEnabled)
    }

    @Test
    fun `MediaCaptureConfig equality is structural`() {
        val a = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Odometer), isOdometer = true, digitLock = 6)
        val b = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Odometer), isOdometer = true, digitLock = 6)

        assertEquals(a, b)
    }

    @Test
    fun `MediaCaptureResult Attachments wraps AttachmentItem list`() {
        val item =
            AttachmentItem(
                id = "1",
                uri = "content://1",
                source = AttachmentSource.CAMERA,
            )
        val result = MediaCaptureResult.Attachments(listOf(item))

        assertEquals(listOf(item), result.items)
    }

    @Test
    fun `MediaCaptureResult Odometer wraps OdometerReading`() {
        val reading = OdometerReading(userReading = "12345")
        val result = MediaCaptureResult.Odometer(reading)

        assertEquals(reading, result.reading)
    }

    @Test
    fun `MediaCaptureResult QrPayload carries raw value`() {
        val result = MediaCaptureResult.QrPayload("QR-VALUE")

        assertEquals("QR-VALUE", result.value)
    }

    @Test
    fun `OdometerReading has sensible defaults`() {
        val reading = OdometerReading()

        assertEquals(null, reading.url)
        assertEquals(0, reading.retakeCount)
        assertEquals(emptyList(), reading.retakeHistory)
        assertEquals(false, reading.isRejected)
    }

    @Test
    fun `OdometerReading equality is structural`() {
        val a = OdometerReading(userReading = "100", retakeCount = 2, retakeHistory = listOf("90", "95"))
        val b = OdometerReading(userReading = "100", retakeCount = 2, retakeHistory = listOf("90", "95"))

        assertEquals(a, b)
    }

    @Test
    fun `AttachmentItem equality is structural`() {
        val a = AttachmentItem(id = "1", uri = "content://1", source = AttachmentSource.GALLERY)
        val b = AttachmentItem(id = "1", uri = "content://1", source = AttachmentSource.GALLERY)

        assertEquals(a, b)
    }
}
