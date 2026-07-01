package com.mileway.core.platform

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android [TextRecognizer] backed by ML Kit on-device Latin text recognition.
 * Decodes the raw image bytes to a bitmap, then runs recognition. Returns "" if the bytes
 * cannot be decoded. The iOS counterpart (Phase 4) uses Vision's `VNRecognizeTextRequest`.
 */
class AndroidTextRecognizer : TextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imageBytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return ""
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
