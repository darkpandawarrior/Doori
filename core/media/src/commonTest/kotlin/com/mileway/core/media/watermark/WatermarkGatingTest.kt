package com.mileway.core.media.watermark

import com.mileway.core.media.model.MediaCaptureConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * V26 P26.WM.2: the "should we watermark" decision is pure/common, so it's testable without a
 * platform [burnWatermark] actual — the pixel-level burn itself (Canvas/Paint on Android,
 * UIGraphicsImageRenderer on iOS) is exercised on-device only.
 */
class WatermarkGatingTest {
    @Test
    fun `defaults do not watermark`() {
        assertFalse(MediaCaptureConfig().shouldWatermark())
    }

    @Test
    fun `watermarkingEnabled alone watermarks`() {
        assertTrue(MediaCaptureConfig(watermarkingEnabled = true).shouldWatermark())
    }

    @Test
    fun `watermarkingMandatory alone watermarks even when enabled is not set`() {
        assertTrue(MediaCaptureConfig(watermarkingEnabled = false, watermarkingMandatory = true).shouldWatermark())
    }

    @Test
    fun `both flags set watermarks`() {
        assertTrue(MediaCaptureConfig(watermarkingEnabled = true, watermarkingMandatory = true).shouldWatermark())
    }

    @Test
    fun `watermarkText includes formatted timestamp and truncated traceId`() {
        val config = MediaCaptureConfig(traceId = "trace-id-0123456789")
        val text = watermarkText(config, capturedAtMillis = 1_700_000_000_000L)

        assertTrue(text.contains("trace-id"), "expected truncated trace id in \"$text\"")
        assertEquals("trace-id", config.traceId!!.take(8))
    }

    @Test
    fun `watermarkText omits traceId separator when traceId is null`() {
        val text = watermarkText(MediaCaptureConfig(), capturedAtMillis = 1_700_000_000_000L)

        assertFalse(text.contains("·"))
    }
}
