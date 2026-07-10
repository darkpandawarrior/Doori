package com.mileway.core.media

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.KeywordHeuristicClassifier
import com.mileway.core.ai.MlKitGenAiAnalyzer
import com.mileway.core.ai.MlKitTextRecognizer
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.media.model.AttachmentItem
import com.mileway.core.media.model.AttachmentSource
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.watermark.burnWatermark
import com.mileway.core.media.watermark.shouldWatermark
import com.mileway.core.media.watermark.watermarkText
import com.mileway.core.ui.components.sheet.BatchOcrItem
import com.mileway.core.ui.components.sheet.BatchOcrStatus
import com.mileway.core.ui.components.sheet.OcrBatchResultsSheet
import com.mileway.core.ui.components.sheet.OcrResultHost
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import kotlinx.coroutines.launch
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
 *
 * V26 P26.SHEET: every branch that produces attachments routes through [deliverAttachments]
 * below instead of calling `onResult` directly — the one place [MediaCaptureConfig.enableOcr] is
 * read, so every mode (gallery/files/pdf/document) gets the same OCR-then-sheet behavior for free.
 */
@Composable
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
    onOcrAnalysis: (DocumentAnalysis) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode = config.allowedModes.firstOrNull() ?: CaptureMode.Gallery

    // P26.SHEET: real DocumentIntelligence, built the same way rememberOdometerOcrService's
    // Android actual does (OdometerOcrService.android.kt) — real ML Kit GenAI + text recognition,
    // no DI module needed for a Compose-scoped construction like this.
    val documentIntelligence =
        remember(context) {
            DocumentIntelligence(
                aiAnalyzer = MlKitGenAiAnalyzer(context),
                textRecognizer = MlKitTextRecognizer(context),
                classifier = KeywordHeuristicClassifier,
            )
        }
    val expectedDocType =
        remember(config.ocrDocType) {
            config.ocrDocType?.let { runCatching { DocType.valueOf(it) }.getOrNull() } ?: DocType.RECEIPT
        }
    val ocrPrompt =
        remember(expectedDocType) {
            DocPrompt(
                docType = expectedDocType,
                instruction = "Extract merchant, total, tax, date and category from this ${expectedDocType.name.lowercase()} image.",
                schemaHint = "{merchant, total, tax, date, invoiceNo, category, currency}",
            )
        }

    // Pending single-item analysis (drives OcrResultHost) and pending batch analysis (drives
    // OcrBatchResultsSheet) — mutually exclusive, only one is non-null at a time. [pendingItems]
    // is what finally reaches [onResult] once the user resolves whichever sheet is showing.
    var singleAnalysis by remember { mutableStateOf<DocumentAnalysis?>(null) }
    var batchItems by remember { mutableStateOf<List<BatchOcrItem>?>(null) }
    var pendingItems by remember { mutableStateOf<List<AttachmentItem>?>(null) }

    fun deliverAttachments(items: List<AttachmentItem>) {
        if (items.isEmpty()) return
        scope.launch {
            // V26 P26.WM.3: burn the watermark (real Canvas/Paint burn-in, not the old
            // "$uri#watermarked" stub) before OCR/delivery so both the OCR pass and the final
            // attachment see the watermarked image.
            val watermarked =
                if (config.shouldWatermark()) {
                    items.map { item -> item.copy(uri = burnWatermark(item.uri, watermarkText(config, item.capturedAtMillis))) }
                } else {
                    items
                }

            if (!config.enableOcr) {
                onResult(MediaCaptureResult.Attachments(watermarked))
                return@launch
            }
            if (watermarked.size == 1) {
                val analysis = runCatching { documentIntelligence.analyze(watermarked[0].uri, ocrPrompt) }.getOrNull()
                if (analysis == null) {
                    // OCR itself failed (not a duplicate/wrong-doc-type verdict) — still attach the
                    // photo rather than blocking the user on an OCR-plumbing error.
                    onResult(MediaCaptureResult.Attachments(watermarked))
                } else {
                    pendingItems = watermarked
                    singleAnalysis = analysis
                }
            } else {
                batchItems =
                    watermarked.map { item ->
                        val analysis = runCatching { documentIntelligence.analyze(item.uri, ocrPrompt) }.getOrNull()
                        BatchOcrItem(label = item.uri.substringAfterLast('/'), status = analysis.toBatchStatus(expectedDocType))
                    }
                pendingItems = watermarked
            }
        }
    }

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
            deliverAttachments(byteArrays.map { it.toCacheFileAttachment(context, AttachmentSource.GALLERY) })
        }

    // Files/Pdf: SAF document picker. content:// uris are handed back as-is — no local decode here,
    // that's OCR's job downstream (RealMediaRepository, fixed for EXIF orientation in P26.AND.4).
    val singleFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) deliverAttachments(listOf(uri.toAttachment(context)))
        }
    val multiFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            deliverAttachments(uris.map { it.toAttachment(context) })
        }

    // Document: GMS scanner with a same-shape file-picker fallback wired on failure — the actual
    // fix for the "silently no-ops when the scanner is unavailable" bug this task calls out.
    val documentFallbackLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            deliverAttachments(uris.map { it.toAttachment(context) })
        }
    val documentScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                deliverAttachments(scan?.pages.orEmpty().map { it.imageUri.toAttachment(context) })
            }
        }

    pendingItems?.let { items ->
        OcrResultHost(
            analysis = singleAnalysis,
            expectedDocType = expectedDocType,
            // ponytail: forwards the original DocumentAnalysis, not the sheet's edited field
            // strings — OcrReviewSheet's edits are UI-local for now. Threading a corrected
            // DocumentAnalysis back through onOcrAnalysis is straightforward (rebuild `fields`
            // from the edited map, keep everything else) but no caller needs it yet; do it when
            // the first real autofill consumer (e.g. feature:logging's expense form) lands.
            onUseData = {
                singleAnalysis?.let(onOcrAnalysis)
                onResult(MediaCaptureResult.Attachments(items))
                singleAnalysis = null
                pendingItems = null
            },
            onIgnore = {
                onResult(MediaCaptureResult.Attachments(items))
                singleAnalysis = null
                pendingItems = null
            },
            onContinueAnyway = {
                onResult(MediaCaptureResult.Attachments(items))
                singleAnalysis = null
                pendingItems = null
            },
            onRetake = {
                singleAnalysis = null
                pendingItems = null
            },
        )
        batchItems?.let { batch ->
            OcrBatchResultsSheet(
                items = batch,
                onDone = {
                    onResult(MediaCaptureResult.Attachments(items))
                    batchItems = null
                    pendingItems = null
                },
            )
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

/**
 * P26.SHEET: one file's [BatchOcrItem] status for [OcrBatchResultsSheet] — [BatchOcrStatus] only
 * has three states, so a wrong-doc-type verdict collapses into [BatchOcrStatus.Failed] alongside a
 * genuine analyzer error (both mean "this file isn't a usable expected-type receipt").
 */
private fun DocumentAnalysis?.toBatchStatus(expectedDocType: DocType): BatchOcrStatus =
    when {
        this == null || docType != expectedDocType -> BatchOcrStatus.Failed
        duplicate != DuplicateVerdict.Unique -> BatchOcrStatus.Duplicate
        else -> BatchOcrStatus.Success
    }
