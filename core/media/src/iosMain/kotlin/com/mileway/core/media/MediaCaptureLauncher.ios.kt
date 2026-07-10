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
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile

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
 * Files/Pdf/Document/Odometer/QRCode/Barcode/CloudLibrary are out of scope for this task
 * (VisionKit document scanner, iOS file picker, QR/barcode, the unified odometer pipeline,
 * cloud-library browsing) — each throws a clear "not yet wired" error, the same shape Android's
 * own out-of-scope modes use.
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

                CaptureMode.Files,
                CaptureMode.Pdf,
                CaptureMode.Document,
                CaptureMode.Odometer,
                CaptureMode.QRCode,
                CaptureMode.Barcode,
                CaptureMode.CloudLibrary,
                ->
                    // ponytail: V26 P-IOS.2/P-QR follow-up — VisionKit document scanner, iOS file
                    // picker, QR/barcode, the odometer pipeline, cloud-library browsing. This task
                    // is camera+gallery only.
                    error(
                        "rememberMediaCaptureLauncher: $mode is not wired on iOS yet " +
                            "(V26 P-IOS.2/P-QR follow-up).",
                    )
            }
        }
    }
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
