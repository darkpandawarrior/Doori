package com.mileway.feature.tracking.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OdometerOcrAnalyzer {
    suspend fun analyze(
        imageUri: Uri,
        context: Context,
    ): OcrResult =
        withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val textRecognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val visionText = textRecognizer.process(inputImage).await()
                textRecognizer.close()

                val rawText = visionText.text
                val reading = extractOdometerReading(rawText)
                if (reading != null) {
                    OcrResult.Success(reading = reading, rawText = rawText)
                } else {
                    OcrResult.Failure("No valid odometer reading in: ${rawText.take(80)}")
                }
            } catch (e: Exception) {
                OcrResult.Failure(e.message ?: "Image processing failed")
            }
        }

    private fun extractOdometerReading(text: String): Int? =
        Regex("\\d{4,7}").findAll(text)
            .mapNotNull { it.value.toIntOrNull() }
            .filter { it in 1_000..999_999 }
            .maxOrNull()
}

sealed interface OcrResult {
    data class Success(val reading: Int, val rawText: String) : OcrResult

    data class Failure(val reason: String) : OcrResult
}
