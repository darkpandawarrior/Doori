package com.mileway.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mileway.core.ai.model.DocumentImageRef
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android [TextRecognizer] backed by ML Kit on-device Latin text recognition (bundled model, no
 * network, no Play Store download), decoding a [DocumentImageRef] (uri/path string) to a bitmap
 * before recognition. Decode mirrors `feature:media`'s `RealMediaRepository.decodeBitmap`:
 * content:// URI via [Context.getContentResolver], falling back to a plain file path.
 *
 * Never throws: any decode or recognition failure resolves to "", which [DocumentIntelligence]
 * treats as "nothing to contribute" rather than a crash.
 */
class MlKitTextRecognizer(private val context: Context) : TextRecognizer {
    // ponytail: lazy, not eager — this class is constructed unconditionally at Compose
    // composition time by every rememberMediaCaptureLauncher call (core:media), whether or not
    // OCR is ever used. TextRecognition.getClient() touches MlKitContext.getInstance(), which
    // isn't initialized in every host (e.g. Robolectric screenshot tests) — deferring construction
    // to the first real recognize() call means a composable that merely mounts the launcher never
    // pays that cost or that crash risk.
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(image: DocumentImageRef): String {
        val bitmap = decodeBitmap(image) ?: return ""
        return try {
            recognizeText(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            recognizer
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resume("") }
        }

    private fun decodeBitmap(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { BitmapFactory.decodeFile(it) } }.getOrNull()
    }
}
