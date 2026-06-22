package com.miletracker.feature.media.ocr

import com.miletracker.feature.media.repository.MediaRepository

/**
 * D.2b: turns a **gallery-picked** image into an odometer reading via the same multi-pass OCR pipeline
 * the camera uses ([MediaRepository.runOcr] → [OdometerOcrAggregator]). Lets the user choose an existing
 * odometer photo instead of taking a new one; the parsed reading + confidence/verification then seed an
 * `OdometerCaptureResult`.
 *
 * Thin, pure adapter over the repository so it is JVM-unit-testable with `FakeMediaRepository`.
 */
class GalleryOdometerProcessor(private val mediaRepository: MediaRepository) {
    data class Reading(
        val value: Int?,
        val confidence: Float,
        val isVerified: Boolean,
        val rawText: String,
    )

    /** Run multi-pass OCR on [uri] and parse out the numeric odometer reading (null if none found). */
    suspend fun process(uri: String): Reading {
        val ocr = mediaRepository.runOcr(uri)
        return Reading(
            value = ocr.detectedOdometer?.toIntOrNull(),
            confidence = ocr.confidence,
            isVerified = ocr.isVerified,
            rawText = ocr.rawText,
        )
    }
}
