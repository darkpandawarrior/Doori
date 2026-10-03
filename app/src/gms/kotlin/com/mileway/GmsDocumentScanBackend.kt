package com.mileway

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mileway.core.media.DocumentScanBackend

/** Play-only scanner with the caller's system-picker fallback on failure. */
class GmsDocumentScanBackend : DocumentScanBackend {
    override fun start(activity: Activity, pageLimit: Int, onReady: (IntentSender) -> Unit, onUnavailable: () -> Unit) {
        val options =
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(pageLimit.coerceAtLeast(1))
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener(onReady)
            .addOnFailureListener { onUnavailable() }
    }

    override fun pages(intent: Intent?): List<String> =
        GmsDocumentScanningResult.fromActivityResultIntent(intent)?.pages.orEmpty().map { it.imageUri.toString() }
}
