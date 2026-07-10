package com.mileway.core.media

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mileway.core.media.model.AttachmentItem
import com.mileway.core.media.model.AttachmentSource
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Default MIME allow-list for [CaptureMode.Files] — the reference app's default doc/spreadsheet
 * set (V26 P26.AND.2). `core:media`'s `MediaCaptureConfig` has no per-call MIME override yet
 * (nothing needs one today); add `allowedMimeTypes` there if/when a caller needs to narrow this.
 */
private val FILES_MIME_TYPES =
    arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "image/*",
    )

private val PDF_MIME_TYPES = arrayOf("application/pdf")

/**
 * Android actual (V26 P26.AND.1-.4): real capture launcher for [CaptureMode.Gallery] (Peekaboo
 * image picker), [CaptureMode.Files]/[CaptureMode.Pdf] (SAF document picker, MIME allow-listed),
 * and [CaptureMode.Document] (GMS document scanner, falling back to the file picker when the
 * scanner is unavailable instead of the previous silent no-op).
 *
 * A [MediaCaptureConfig] is expected to request exactly one mode via `allowedModes` — the first
 * (declaration order) wins if more than one is set; a single tap-driven source picker across
 * several modes is a P-SITE call-site concern, not this launcher's.
 *
 * [CaptureMode.Camera] stays on `feature:media`'s existing `CameraCaptureScreen` (a full-screen
 * preview, not a single-shot launcher) and Odometer/QRCode/Barcode/CloudLibrary are V26
 * P-CONV/P-QR/P-LIB — all four throw a clear "not yet wired" error rather than silently no-op.
 */
@Composable
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode = config.allowedModes.firstOrNull() ?: CaptureMode.Gallery

    // Gallery: Peekaboo already EXIF-corrects + JPEG-encodes each pick (P26.AND.4 for this path);
    // we only need to persist the bytes to a cache file for a stable uri AttachmentItem can carry.
    val galleryLauncher =
        rememberImagePickerLauncher(
            selectionMode =
                if (config.multiple) {
                    SelectionMode.Multiple(maxSelection = config.maxCount)
                } else {
                    SelectionMode.Single
                },
            scope = scope,
        ) { byteArrays ->
            val items = byteArrays.map { it.toCacheFileAttachment(context, AttachmentSource.GALLERY) }
            if (items.isNotEmpty()) onResult(MediaCaptureResult.Attachments(items))
        }

    // Files/Pdf: SAF document picker. content:// uris are handed back as-is — no local decode here,
    // that's OCR's job downstream (RealMediaRepository, fixed for EXIF orientation in P26.AND.4).
    val singleFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onResult(MediaCaptureResult.Attachments(listOf(uri.toAttachment(context))))
        }
    val multiFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                onResult(MediaCaptureResult.Attachments(uris.map { it.toAttachment(context) }))
            }
        }

    // Document: GMS scanner with a same-shape file-picker fallback wired on failure — the actual
    // fix for the "silently no-ops when the scanner is unavailable" bug this task calls out.
    val documentFallbackLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                onResult(MediaCaptureResult.Attachments(uris.map { it.toAttachment(context) }))
            }
        }
    val documentScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val items = scan?.pages.orEmpty().map { it.imageUri.toAttachment(context) }
                if (items.isNotEmpty()) onResult(MediaCaptureResult.Attachments(items))
            }
        }

    return remember(mode, config, context) {
        {
            when (mode) {
                CaptureMode.Gallery -> galleryLauncher.launch()

                CaptureMode.Files ->
                    launchFilePicker(config, FILES_MIME_TYPES, singleFileLauncher, multiFileLauncher)

                CaptureMode.Pdf ->
                    launchFilePicker(config, PDF_MIME_TYPES, singleFileLauncher, multiFileLauncher)

                CaptureMode.Document -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        // No Activity host to launch an IntentSender from — same fallback as an
                        // unavailable scanner.
                        documentFallbackLauncher.launch(FILES_MIME_TYPES)
                    } else {
                        val options =
                            GmsDocumentScannerOptions.Builder()
                                .setGalleryImportAllowed(true)
                                .setPageLimit(if (config.multiple) config.maxCount.coerceAtLeast(1) else 1)
                                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                .build()
                        GmsDocumentScanning.getClient(options)
                            .getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                documentScanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener {
                                // ponytail: scanner unavailable (no Play Services / unsupported
                                // device) — fall back to the file picker instead of no-op.
                                documentFallbackLauncher.launch(FILES_MIME_TYPES)
                            }
                    }
                }

                CaptureMode.Camera,
                CaptureMode.Odometer,
                CaptureMode.QRCode,
                CaptureMode.Barcode,
                CaptureMode.CloudLibrary,
                ->
                    error(
                        "rememberMediaCaptureLauncher: $mode is not wired through this launcher yet " +
                            "(Camera uses feature:media's CameraCaptureScreen directly; " +
                            "Odometer/QRCode/Barcode/CloudLibrary land in V26 P-CONV/P-QR/P-LIB).",
                    )
            }
        }
    }
}

private fun launchFilePicker(
    config: MediaCaptureConfig,
    mimeTypes: Array<String>,
    single: ActivityResultLauncher<Array<String>>,
    multi: ActivityResultLauncher<Array<String>>,
) {
    if (config.multiple) multi.launch(mimeTypes) else single.launch(mimeTypes)
}

private fun ByteArray.toCacheFileAttachment(
    context: Context,
    source: AttachmentSource,
): AttachmentItem {
    val file = File(context.cacheDir, "capture_${UUID.randomUUID()}.jpg")
    FileOutputStream(file).use { it.write(this) }
    return AttachmentItem(
        id = UUID.randomUUID().toString(),
        uri = Uri.fromFile(file).toString(),
        source = source,
        mimeType = "image/jpeg",
        capturedAtMillis = System.currentTimeMillis(),
    )
}

private fun Uri.toAttachment(
    context: Context,
    source: AttachmentSource = AttachmentSource.FILES,
): AttachmentItem {
    val mime = context.contentResolver.getType(this) ?: "application/octet-stream"
    return AttachmentItem(
        id = UUID.randomUUID().toString(),
        uri = toString(),
        source = source,
        mimeType = mime,
        capturedAtMillis = System.currentTimeMillis(),
    )
}
