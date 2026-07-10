package com.mileway.feature.media.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mileway.core.media.model.UploadState
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.OcrResult
import com.mileway.feature.media.ocr.OdometerOcrAggregator
import com.mileway.feature.media.ocr.OdometerOcrParser
import kotlinx.coroutines.tasks.await

/**
 * Production [MediaRepository] running on-device ML Kit text recognition (bundled Latin model, no
 * network, no Play Store download — works in both flavors).
 *
 * D.2 multi-pass OCR: a single recogniser pass over a raw photo is fragile (glare, low light, blur).
 * [runOcr] now decodes the frame once and runs the recogniser over several **enhancement variants**
 * (default, high-contrast, grayscale, brightened); each pass's parsed reading is fed to the pure
 * [OdometerOcrAggregator], which returns the most-agreed reading, an agreement-based confidence, and an
 * `isVerified` flag (>=2 passes agreed). Falls back to a "no reading" result on any failure so the user
 * can type the value — the app never crashes.
 */
class RealMediaRepository(private val context: Context) : MediaRepository {
    override suspend fun runOcr(uri: String): OcrResult {
        return try {
            val source =
                decodeBitmapCorrected(uri)
                    ?: return OcrResult("Could not decode image.", null, 0f, false)

            val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val passes = mutableListOf<OdometerOcrAggregator.PassResult>()
            var rawText = ""
            try {
                for ((name, transform) in VARIANTS) {
                    val variantBitmap = transform(source)
                    val recognised = recogniser.process(InputImage.fromBitmap(variantBitmap, 0)).await()
                    if (variantBitmap != source) variantBitmap.recycle()

                    val lines = recognised.textBlocks.flatMap { block -> block.lines.map { it.text } }
                    if (rawText.isBlank()) rawText = recognised.text
                    val reading = OdometerOcrParser.parse(lines)
                    passes +=
                        OdometerOcrAggregator.PassResult(
                            variant = name,
                            reading = reading,
                            labelled = reading != null && isLabelledHit(lines, reading),
                        )
                }
            } finally {
                recogniser.close()
                source.recycle()
            }

            val aggregate = OdometerOcrAggregator.aggregate(passes)
            OcrResult(
                rawText = rawText.ifBlank { "No text detected." },
                detectedOdometer = aggregate.reading,
                confidence = aggregate.confidence,
                watermarkApplied = false,
                passCount = aggregate.totalPasses,
                isVerified = aggregate.isVerified,
            )
        } catch (e: Exception) {
            OcrResult("Recognition failed: ${e.message}", null, 0f, false)
        }
    }

    override suspend fun applyWatermark(
        uri: String,
        text: String,
    ): String {
        // Stub: watermarking is out of scope for offline OCR.
        return "$uri#watermarked"
    }

    override suspend fun upload(item: AttachmentItem): UploadState.Done {
        // Stub: upload is intentionally offline-only in this demo.
        return UploadState.Done("local://${item.id}")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun decodeBitmap(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { BitmapFactory.decodeFile(it) } }.getOrNull()
    }

    /**
     * V26 P26.AND.4: [decodeBitmap] plus EXIF orientation correction. A camera/gallery/document-
     * scan JPEG commonly carries an EXIF orientation tag rather than pre-rotated pixels — OCR
     * (and any thumbnail reading pixels directly) needs the corrected bitmap, not the raw one.
     * Peekaboo's gallery picker already bakes this in for its own bytes (V26 P26.AND.1); this is
     * the one shared decode path every OCR call — camera, gallery, document-scan, files — funnels
     * through, so fixing it here fixes all of them at once.
     */
    private fun decodeBitmapCorrected(uri: String): Bitmap? {
        val bitmap = decodeBitmap(uri) ?: return null
        return applyExifOrientation(bitmap, readExifOrientation(uri))
    }

    private fun readExifOrientation(uri: String): Int {
        val parsed = Uri.parse(uri)
        val fromResolver =
            runCatching {
                context.contentResolver.openInputStream(parsed)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull()
        return fromResolver
            ?: runCatching {
                parsed.path?.let { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            }.getOrNull()
            ?: ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyExifOrientation(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** True when [detected] sits on a line that also carries an odometer label — a higher-confidence hit. */
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

    private companion object {
        /** Enhancement variants run per frame; order is the pass order. "default" returns the source as-is. */
        val VARIANTS: List<Pair<String, (Bitmap) -> Bitmap>> =
            listOf(
                "default" to { src: Bitmap -> src },
                "high_contrast" to { src: Bitmap -> src.filtered(contrastMatrix(1.6f)) },
                "grayscale" to { src: Bitmap -> src.filtered(ColorMatrix().apply { setSaturation(0f) }) },
                "brighten" to { src: Bitmap -> src.filtered(brightnessMatrix(1.4f)) },
            )

        fun Bitmap.filtered(matrix: ColorMatrix): Bitmap {
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(this, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
            return out
        }

        /** Contrast around mid-grey: scale each channel by [c] and re-centre. */
        fun contrastMatrix(c: Float): ColorMatrix {
            val translate = (1f - c) / 2f * 255f
            return ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }

        /** Brighten (low-light boost): scale RGB by [b]. */
        fun brightnessMatrix(b: Float): ColorMatrix =
            ColorMatrix(
                floatArrayOf(
                    b, 0f, 0f, 0f, 0f,
                    0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
    }
}
