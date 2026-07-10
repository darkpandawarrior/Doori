package com.mileway.core.media.watermark

import com.mileway.core.media.model.MediaCaptureConfig
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Burns [text] into the image at [imageUri]'s bottom-right corner, over a semi-transparent
 * legibility strip so it stays readable against any photo. Returns the new watermarked image's
 * uri; returns [imageUri] unchanged if decoding/writing fails — watermarking must never block a
 * capture (V26 P26.WM.1 Android `Canvas`/`Paint` actual, P26.WM.2 iOS `UIGraphicsImageRenderer`
 * actual).
 */
expect suspend fun burnWatermark(
    imageUri: String,
    text: String,
): String

/**
 * V26 P26.WM.3: whether [MediaCaptureConfig] wants its captured attachments watermarked.
 * `watermarkingMandatory` implies `watermarkingEnabled` — a caller that requires the watermark
 * obviously wants it applied even if it only set the mandatory flag.
 */
fun MediaCaptureConfig.shouldWatermark(): Boolean = watermarkingEnabled || watermarkingMandatory

/**
 * V26 P26.WM.3: the short legibility-strip line — a timestamp plus [MediaCaptureConfig.traceId]
 * (truncated; a trace-id is an identifier, not label copy, so a short prefix is enough on-image).
 */
fun watermarkText(
    config: MediaCaptureConfig,
    capturedAtMillis: Long,
): String =
    listOfNotNull(formatTimestamp(capturedAtMillis), config.traceId?.take(TRACE_ID_DISPLAY_LENGTH))
        .joinToString(" · ")

private const val TRACE_ID_DISPLAY_LENGTH = 8

private fun formatTimestamp(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

    fun Int.pad() = toString().padStart(2, '0')
    return "${dt.year}-${dt.month.number.pad()}-${dt.day.pad()} ${dt.hour.pad()}:${dt.minute.pad()}"
}
