package com.mileway.core.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.media.model.AttachmentItem
import com.mileway.core.media.model.AttachmentSource
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.ui.camera.PeekabooCamera
import com.preat.peekaboo.ui.camera.rememberPeekabooCameraState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.VisionKit.VNDocumentCameraScan
import platform.VisionKit.VNDocumentCameraViewController
import platform.VisionKit.VNDocumentCameraViewControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS actual (V26 P26.IOS.1): real capture launcher for [CaptureMode.Gallery] (Peekaboo image
 * picker — `peekaboo-image-picker` is genuinely KMP, same `rememberImagePickerLauncher` API the
 * Android actual uses) and [CaptureMode.Camera] (peekaboo-ui's `PeekabooCamera`).
 *
 * `PeekabooCamera` is a live-preview *composable*, not an imperative one-shot launcher — there is
 * no iOS equivalent of `rememberLauncherForActivityResult` for a full-screen camera. So the
 * `Camera` branch of the returned trigger just flips a `showCamera` flag, and this function emits
 * a full-screen `Dialog` hosting `PeekabooCamera` while that flag is set (Dialog composes on top
 * of whatever the caller is rendering, independent of where in the tree this `remember*` call
 * sits — the same `Dialog` idiom `core:ui`'s `ZoomImageViewer`/`ColorWheelDialog` already use).
 *
 * [CaptureMode.QRCode]/[CaptureMode.Barcode] are real as of V26 P26.IOS.3 — see [scanBarcode]
 * (Vision's `VNDetectBarcodesRequest`, same image-then-decode shape as the Android actual).
 * [CaptureMode.Document] is real as of V26 P26.IOS.2: `VNDocumentCameraViewController`
 * (VisionKit) presented from the front-most view controller (same "no Compose-provided
 * `UIViewController` needed" trick `IosShareSheet`/`ExpenseCsvImportLauncher.ios.kt`'s
 * `UIDocumentPickerViewController` already use), scanned pages read back via
 * `VNDocumentCameraScan.imageOfPageAtIndex` and JPEG-encoded into cache-file attachments —
 * see [DocumentScanDelegate]. This finally closes the gap `core:platform`'s `IosDocumentScanner`
 * doc comment named (headless service objects have no presentation context; the Compose UI layer
 * does).
 * Files/Pdf/Odometer/CloudLibrary are still out of scope for this task (iOS file picker, the
 * unified odometer pipeline, cloud-library browsing) — each throws a clear "not yet wired" error,
 * the same shape Android's own out-of-scope modes use.
 *
 * ponytail: `config.enableOcr`/[onOcrAnalysis] aren't wired here yet — `core:ai`'s
 * `DocumentAiAnalyzer` has a real iOS actual (`FoundationModelsAnalyzer`), but threading it
 * through Peekaboo's byte-array callbacks is real follow-up work (V26 P-IOS.2), not this task's
 * Android-only real path (see `MediaCaptureLauncher.android.kt`).
 */
@Composable
actual fun rememberMediaCaptureLauncher(
    config: MediaCaptureConfig,
    onResult: (MediaCaptureResult) -> Unit,
    onOcrAnalysis: (DocumentAnalysis) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val mode = config.allowedModes.firstOrNull() ?: CaptureMode.Gallery
    var showCamera by remember { mutableStateOf(false) }

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
            val items = byteArrays.map { it.toCacheFileAttachment(AttachmentSource.GALLERY) }
            if (items.isNotEmpty()) onResult(MediaCaptureResult.Attachments(items))
        }

    val cameraState =
        rememberPeekabooCameraState { bytes ->
            showCamera = false
            val captured = bytes ?: return@rememberPeekabooCameraState
            onResult(MediaCaptureResult.Attachments(listOf(captured.toCacheFileAttachment(AttachmentSource.CAMERA))))
        }

    // QRCode/Barcode (V26 P26.IOS.3): same image-then-decode shape as the Android actual — pick a
    // still image via Peekaboo, decode it with Vision's VNDetectBarcodesRequest.
    // ponytail: not a live viewfinder scanner, same ceiling as the Android actual (see its
    // scanBarcode doc) — upgrade both together if a live scan is ever needed.
    val qrLauncher =
        rememberImagePickerLauncher(
            selectionMode = SelectionMode.Single,
            scope = scope,
        ) { byteArrays ->
            val bytes = byteArrays.firstOrNull() ?: return@rememberImagePickerLauncher
            val value = scanBarcode(bytes)
            if (value != null) onResult(MediaCaptureResult.QrPayload(value))
        }

    val documentScanDelegate =
        remember {
            DocumentScanDelegate { pages ->
                val items = pages.map { it.toCacheFileAttachment(AttachmentSource.FILES) }
                if (items.isNotEmpty()) onResult(MediaCaptureResult.Attachments(items))
            }
        }

    if (showCamera) {
        Dialog(onDismissRequest = { showCamera = false }) {
            PeekabooCamera(state = cameraState, modifier = Modifier.fillMaxSize())
        }
    }

    return remember(mode, config) {
        {
            when (mode) {
                CaptureMode.Gallery -> galleryLauncher.launch()
                CaptureMode.Camera -> showCamera = true
                CaptureMode.QRCode, CaptureMode.Barcode -> qrLauncher.launch()

                CaptureMode.Document -> {
                    val scanner = VNDocumentCameraViewController()
                    scanner.delegate = documentScanDelegate
                    topViewController()?.presentViewController(scanner, animated = true, completion = null)
                }

                CaptureMode.Files,
                CaptureMode.Pdf,
                CaptureMode.Odometer,
                CaptureMode.CloudLibrary,
                ->
                    // ponytail: V26 P-IOS.2 follow-up — iOS file picker, the unified odometer
                    // pipeline, cloud-library browsing. Document scanning is real now (see above).
                    error(
                        "rememberMediaCaptureLauncher: $mode is not wired on iOS yet " +
                            "(V26 P-IOS.2 follow-up).",
                    )
            }
        }
    }
}

/** Decodes the first QR/barcode found in [bytes] via Vision, or null if none is detected. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun scanBarcode(bytes: ByteArray): String? =
    runCatching {
        if (bytes.isEmpty()) return@runCatching null
        val data =
            bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
        val cgImage = UIImage(data = data).CGImage ?: return@runCatching null

        val request = VNDetectBarcodesRequest(completionHandler = null)
        val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())
        handler.performRequests(listOf(request), error = null)

        request.results
            .orEmpty()
            .filterIsInstance<VNBarcodeObservation>()
            .firstNotNullOfOrNull { it.payloadStringValue }
    }.getOrNull()

/**
 * [VNDocumentCameraViewControllerDelegateProtocol] adapter: dismisses the scanner on every
 * outcome (finish/cancel/fail — VisionKit never dismisses itself) and, on a successful scan,
 * reads each page back as JPEG bytes via `imageOfPageAtIndex` before handing them to [onScanned].
 * Cancel and failure both report zero pages, same "no result" contract as the Android actual's
 * scanner-unavailable fallback.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DocumentScanDelegate(
    private val onScanned: (List<ByteArray>) -> Unit,
) : NSObject(), VNDocumentCameraViewControllerDelegateProtocol {
    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFinishWithScan: VNDocumentCameraScan,
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        val pages =
            (0 until didFinishWithScan.pageCount.toInt()).mapNotNull { index ->
                val image = didFinishWithScan.imageOfPageAtIndex(index.toULong())
                UIImageJPEGRepresentation(image, 0.9)?.toByteArray()
            }
        onScanned(pages)
    }

    override fun documentCameraViewControllerDidCancel(controller: VNDocumentCameraViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
    }

    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFailWithError: NSError,
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
    }
}

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned -> platform.posix.memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toCacheFileAttachment(source: AttachmentSource): AttachmentItem {
    val data =
        if (isEmpty()) {
            NSData()
        } else {
            usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
        }
    val path = NSTemporaryDirectory() + "capture_${NSUUID().UUIDString}.jpg"
    data.writeToFile(path, atomically = true)
    return AttachmentItem(
        id = NSUUID().UUIDString,
        uri = "file://$path",
        source = source,
        mimeType = "image/jpeg",
        capturedAtMillis = (NSDate().timeIntervalSince1970 * 1000.0).toLong(),
    )
}
