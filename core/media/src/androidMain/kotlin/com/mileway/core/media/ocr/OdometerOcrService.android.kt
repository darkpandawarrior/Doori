package com.mileway.core.media.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.KeywordHeuristicClassifier
import com.mileway.core.ai.MlKitGenAiAnalyzer
import com.mileway.core.ai.MlKitTextRecognizer
import com.mileway.core.ai.model.DocumentImageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Android multi-pass gallery recognizer (V26 P26.CONV.1) — the real replacement for the retired
 * `feature:media` `GalleryOdometerProcessor`/`RealMediaRepository.runOcr` multi-pass path. Decodes
 * the picked image once and runs ML Kit over several enhancement variants (default, high-contrast,
 * grayscale, brighten) — each pass becomes one [OcrAggregator.FrameCandidate], `labelled` when the
 * detected reading sits on a line that also carries an odometer keyword — then votes via the same
 * [OcrAggregator] every other odometer-OCR path uses.
 */
class MlKitGalleryMultiPassRecognizer(
    private val context: Context,
) : GalleryMultiPassRecognizer {
    override suspend fun recognize(image: DocumentImageRef): OcrAggregator.AggregateResult =
        withContext(Dispatchers.Default) {
            val source = decodeBitmap(image) ?: return@withContext OcrAggregator.AggregateResult(null, 0f, 0, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val candidates = mutableListOf<OcrAggregator.FrameCandidate>()
            try {
                for ((_, transform) in VARIANTS) {
                    val variant = transform(source)
                    val recognized = recognizer.process(InputImage.fromBitmap(variant, 0)).await()
                    if (variant != source) variant.recycle()

                    val text = recognized.text
                    val reading = OcrAggregator.extractReading(text)
                    candidates +=
                        OcrAggregator.FrameCandidate(
                            rawText = text,
                            quality = recognized.toQualityMetrics(),
                            labelled = reading != null && recognized.isLabelledHit(reading),
                        )
                }
            } finally {
                recognizer.close()
                source.recycle()
            }
            OcrAggregator.aggregate(candidates)
        }

    private fun decodeBitmap(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { BitmapFactory.decodeFile(it) } }.getOrNull()
    }

    /** True when [reading] sits on a line that also carries an odometer label — a higher-confidence hit. */
    private fun Text.isLabelledHit(reading: Int): Boolean {
        val labels = setOf("odo", "odometer", "km", "miles", "mi", "reading", "mileage")
        return textBlocks.flatMap { it.lines }.any { line ->
            val lower = line.text.lowercase()
            line.text.contains(reading.toString()) && labels.any { lower.contains(it) }
        }
    }

    /**
     * ML Kit doesn't expose sharpness/contrast, so those are proxied from text density (more
     * recognized characters per line = a clearer image).
     */
    private fun Text.toQualityMetrics(): FrameQualityAnalyzer.FrameMetrics {
        val lines = textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) return FrameQualityAnalyzer.FrameMetrics(0f, 0f, 0f)
        val densityScore = (lines.map { it.text.length }.average().toFloat() / 12f).coerceIn(0f, 1f)
        return FrameQualityAnalyzer.FrameMetrics(sharpness = densityScore, contrast = densityScore, textConfidence = densityScore)
    }

    private companion object {
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

@Composable
actual fun rememberOdometerOcrService(): OdometerOcrService {
    val context = LocalContext.current
    return remember(context) {
        OdometerOcrService(
            textRecognizer = MlKitTextRecognizer(context),
            galleryRecognizer = MlKitGalleryMultiPassRecognizer(context),
            documentIntelligence =
                DocumentIntelligence(
                    aiAnalyzer = MlKitGenAiAnalyzer(context),
                    textRecognizer = MlKitTextRecognizer(context),
                    classifier = KeywordHeuristicClassifier,
                ),
        )
    }
}
