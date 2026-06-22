package com.miletracker.feature.media.ui.scanner

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * D.4: ML Kit document scanner as a **Compose launcher** rather than a headless DI service.
 *
 * `GmsDocumentScanning` returns an `IntentSender` that must be launched from an Activity and delivers its
 * pages through `onActivityResult` — it is inherently a UI flow (the same reason the `DocumentScanner`
 * service stays a documented no-op; see `IosDocumentScanner`). This hook lives in the UI layer, mirroring
 * camera capture. The bundled ML Kit document-scanner model works on both flavors (allowlisted prime
 * feature), so no gms/noGms split is needed.
 *
 * @return a callback that opens the scanner; scanned page image URIs are delivered to [onScanned].
 */
@Composable
fun rememberDocumentScanLauncher(
    pageLimit: Int = 5,
    onScanned: (List<String>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val resultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val pages = scan?.pages?.map { it.imageUri.toString() }.orEmpty()
                if (pages.isNotEmpty()) onScanned(pages)
            }
        }

    return remember(context, pageLimit, resultLauncher) {
        {
            val activity = context as? Activity
            if (activity != null) {
                val options =
                    GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(pageLimit)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()
                GmsDocumentScanning.getClient(options)
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        resultLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
            }
        }
    }
}
