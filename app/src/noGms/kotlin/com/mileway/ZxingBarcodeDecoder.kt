package com.mileway

import android.graphics.BitmapFactory
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.mileway.core.media.BarcodeDecoder

/**
 * noGms/F-Droid flavor [BarcodeDecoder]: ZXing (Apache-2.0, no Play Services), the FOSS
 * equivalent of the gms flavor's [MlKitBarcodeDecoder]. Same still-image decode shape as the
 * picked-image flow in `MediaCaptureLauncher.android.kt` — not a live CameraX viewfinder scan
 * (that upgrade is already flagged as a separate seam there, ponytail comment on `qrLauncher`).
 */
class ZxingBarcodeDecoder : BarcodeDecoder {
    override suspend fun decode(imageBytes: ByteArray): String? {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            MultiFormatReader().decode(binaryBitmap).text
        } catch (e: NotFoundException) {
            null
        }
    }
}
