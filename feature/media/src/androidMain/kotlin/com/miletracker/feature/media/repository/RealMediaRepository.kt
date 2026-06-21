package com.miletracker.feature.media.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.miletracker.feature.media.model.AttachmentItem
import com.miletracker.feature.media.model.OcrResult
import com.miletracker.feature.media.model.UploadState
import com.miletracker.feature.media.ocr.OdometerOcrParser
import kotlinx.coroutines.tasks.await

/**
 * Production [MediaRepository] that runs on-device ML Kit text recognition
 * (bundled Latin model, no network required, no Play Store download).
 *
 * OCR path:
 *  1. Decode the image URI into an [InputImage] via [InputImage.fromFilePath].
 *  2. Run [TextRecognition] with [TextRecognizerOptions.DEFAULT_OPTIONS] (Latin, bundled).
 *  3. Pass the recognised text lines to [OdometerOcrParser.parse] for number extraction.
 *  4. Return an [OcrResult] with the extracted reading and a confidence derived from
 *     how the extraction succeeded (labelled hit > unlabelled hit > no reading).
 *
 * Graceful fallback: any exception from ML Kit (bad URI, decode failure, etc.) is caught
 * and surfaced as a "no reading detected" [OcrResult] so the user can enter the value
 * manually: the app never crashes.
 *
 * The [applyWatermark] and [upload] methods retain the same offline-stub behaviour as
 * [FakeMediaRepository] because those features are out of scope for this task.
 */
class RealMediaRepository(private val context: Context) : MediaRepository {
    override suspend fun runOcr(uri: String): OcrResult {
        return try {
            val inputImage = InputImage.fromFilePath(context, Uri.parse(uri))
            val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val textResult =
                try {
                    recogniser.process(inputImage).await()
                } finally {
                    recogniser.close()
                }

            val rawText = textResult.text
            val lines =
                textResult.textBlocks
                    .flatMap { block -> block.lines.map { it.text } }

            val detected = OdometerOcrParser.parse(lines)
            val confidence =
                when {
                    detected == null -> 0f
                    isLabelledHit(lines, detected) -> 0.90f
                    else -> 0.65f
                }

            OcrResult(
                rawText = rawText.ifBlank { "No text detected." },
                detectedOdometer = detected,
                confidence = confidence,
                watermarkApplied = false,
            )
        } catch (e: Exception) {
            OcrResult(
                rawText = "Recognition failed: ${e.message}",
                detectedOdometer = null,
                confidence = 0f,
                watermarkApplied = false,
            )
        }
    }

    override suspend fun applyWatermark(
        uri: String,
        text: String,
    ): String {
        // Stub: watermarking is out of scope for offline OCR task.
        return "$uri#watermarked"
    }

    override suspend fun upload(item: AttachmentItem): UploadState.Done {
        // Stub: upload is intentionally offline-only in this demo.
        return UploadState.Done("local://${item.id}")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true when the detected reading appears on a line that also contains an
     * odometer-related label (km, odo, etc.), indicating a higher-confidence hit.
     */
    private fun isLabelledHit(
        lines: List<String>,
        detected: String,
    ): Boolean {
        val labels = setOf("odo", "odometer", "km", "miles", "mi", "reading", "mileage")
        return lines.any { line ->
            val lower = line.lowercase()
            line.contains(detected) && labels.any { lower.contains(it) }
        }
    }
}
