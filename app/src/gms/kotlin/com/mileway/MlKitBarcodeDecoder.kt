package com.mileway

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.mileway.core.media.BarcodeDecoder
import kotlinx.coroutines.tasks.await

/** gms flavor [BarcodeDecoder]: ML Kit's on-device BarcodeScanning (moved here from core:media's
 * androidMain — see BarcodeDecoder.kt kdoc for why). */
class MlKitBarcodeDecoder : BarcodeDecoder {
    override suspend fun decode(imageBytes: ByteArray): String? {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        val scanner = BarcodeScanning.getClient()
        return try {
            val barcodes = scanner.process(InputImage.fromBitmap(bitmap, 0)).await()
            barcodes.firstNotNullOfOrNull { it.rawValue ?: it.displayValue }
        } finally {
            scanner.close()
            bitmap.recycle()
        }
    }
}
