package com.mileway.core.media.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Android actual (V26 P26.WM.1): draws a semi-transparent legibility strip across the bottom of
 * the image and burns [text] onto it (white, bold, right-aligned) via `Canvas`/`Paint`. Writes the
 * result to a new cache file and returns its uri.
 *
 * This is a plain suspend fun with no Compose scope to pull `LocalContext.current` from, so the
 * `Context` needed to read/write the image is resolved through Koin's global accessor —
 * `KoinPlatform.getKoin()` — the same pattern `TrackingTileService`/`MileageSummaryWidget` already
 * use for framework-instantiated components Koin can't constructor-inject. Never throws:
 * watermarking must not block a capture, so any failure (no Koin-registered Context, a bad uri, a
 * decode/encode error) falls back to returning [imageUri] unchanged.
 */
actual suspend fun burnWatermark(
    imageUri: String,
    text: String,
): String {
    val context = KoinPlatform.getKoin().getOrNull<Context>() ?: return imageUri
    val source = decodeBitmap(context, imageUri) ?: return imageUri
    return try {
        val watermarked = source.drawLegibilityStripAndText(text)
        writeToCacheFile(context, watermarked)
    } catch (e: Exception) {
        imageUri
    } finally {
        source.recycle()
    }
}

private fun decodeBitmap(
    context: Context,
    uri: String,
): Bitmap? =
    runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

/** Bottom-right corner, semi-transparent strip behind the text so it reads on any photo. */
private fun Bitmap.drawLegibilityStripAndText(text: String): Bitmap {
    val out = copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)

    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = out.width * 0.035f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
    val padding = textPaint.textSize * 0.6f
    val stripHeight = textPaint.textSize + padding * 2f
    val stripPaint = Paint().apply { color = Color.argb(140, 0, 0, 0) }

    canvas.drawRect(0f, out.height - stripHeight, out.width.toFloat(), out.height.toFloat(), stripPaint)
    canvas.drawText(text, out.width - padding, out.height - padding, textPaint)
    return out
}

private fun writeToCacheFile(
    context: Context,
    bitmap: Bitmap,
): String {
    val file = File(context.cacheDir, "watermark_${UUID.randomUUID()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    bitmap.recycle()
    return Uri.fromFile(file).toString()
}
