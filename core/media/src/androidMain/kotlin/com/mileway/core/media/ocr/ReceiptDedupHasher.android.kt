package com.mileway.core.media.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.InputStream

/** Bounds-first decode limits the longer edge to 256 pixels before the final luma sampling. */
@Suppress("SwallowedException", "TooGenericExceptionCaught")
actual suspend fun sampleReceiptLuma(uri: String): IntArray? =
    withContext(Dispatchers.IO) {
        try {
            val context = KoinPlatform.getKoin().get<Context>()
            val parsed = Uri.parse(uri)

            fun open(): InputStream? =
                when (parsed.scheme) {
                    "content" -> context.contentResolver.openInputStream(parsed)
                    "file", null -> File(parsed.path ?: uri).inputStream()
                    else -> null
                }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = open() ?: return@withContext null
            boundsStream.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
            options.inJustDecodeBounds = false
            options.inSampleSize = MIN_SAMPLE_SIZE
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > MAX_DECODE_EDGE) {
                options.inSampleSize *= 2
            }
            val bitmap = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext null
            try {
                val small = Bitmap.createScaledBitmap(bitmap, ReceiptDedupHasher.WIDTH, ReceiptDedupHasher.HEIGHT, true)
                try {
                    IntArray(ReceiptDedupHasher.WIDTH * ReceiptDedupHasher.HEIGHT) { index ->
                        val pixel = small.getPixel(index % ReceiptDedupHasher.WIDTH, index / ReceiptDedupHasher.WIDTH)
                        (Color.red(pixel) * RED_WEIGHT + Color.green(pixel) * GREEN_WEIGHT + Color.blue(pixel) * BLUE_WEIGHT) / LUMA_WEIGHT_SUM
                    }
                } finally {
                    if (small !== bitmap) small.recycle()
                }
            } finally {
                bitmap.recycle()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ignored: Exception) {
            // Missing files, denied URI permissions and unsupported images skip image evidence.
            null
        }
    }

private const val MAX_DECODE_EDGE = 256
private const val MIN_SAMPLE_SIZE = 2

private const val RED_WEIGHT = 299
private const val GREEN_WEIGHT = 587
private const val BLUE_WEIGHT = 114
private const val LUMA_WEIGHT_SUM = 1000
