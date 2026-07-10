package com.mileway.core.media.model

/**
 * A single odometer capture attempt's outcome (V25 P25.A1.1) — the retake history and OCR readings
 * feed the odometer-verification flow (device OCR vs on-device AI OCR vs user override).
 */
data class OdometerReading(
    val url: String? = null,
    val userReading: String? = null,
    val deviceOcrReading: String? = null,
    val aiOcrReading: String? = null,
    val retakeCount: Int = 0,
    val retakeHistory: List<String> = emptyList(),
    val isRejected: Boolean = false,
    val rejectionReason: String? = null,
)

/** Terminal result of a media-capture flow launched via `rememberMediaCaptureLauncher` (V25 P25.A1.1/.2). */
sealed interface MediaCaptureResult {
    data class Attachments(val items: List<AttachmentItem>) : MediaCaptureResult

    data class Odometer(val reading: OdometerReading) : MediaCaptureResult

    data class QrPayload(val value: String) : MediaCaptureResult
}
