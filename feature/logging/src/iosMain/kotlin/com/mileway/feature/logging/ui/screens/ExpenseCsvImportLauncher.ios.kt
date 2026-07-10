package com.mileway.feature.logging.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * iOS actual (P27.E.14): `UIDocumentPickerViewController` (the system Files picker), presented
 * from the top-most root view controller — the same "no Compose-provided `UIViewController`
 * needed" trick `IosShareSheet` already uses for `UIActivityViewController`. That's what closes
 * the gap the previous no-op here documented: VisionKit's document *scanner* genuinely needs a
 * live view controller handed in from the Compose layer (still true, see `IosDocumentScanner`),
 * but presenting a picker only needs *some* front-most controller, and `UIApplication`'s
 * key-window root controller is always that.
 *
 * Uses the deprecated `documentTypes:inMode:` initializer (UTI strings), not the newer
 * `UTType`-based one — `core:media`'s `CaptureMode.Files` (the richer, `UTType`-based picker) is
 * still the deferred V26 P-IOS.2 follow-up; this is a narrower, self-contained CSV/text picker
 * that doesn't need that framework.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberExpenseCsvImportLauncher(onPicked: (String) -> Unit): () -> Unit {
    val delegate = remember { CsvDocumentPickerDelegate(onPicked) }
    return remember(delegate) {
        {
            @Suppress("DEPRECATION")
            val picker =
                UIDocumentPickerViewController(
                    documentTypes = listOf("public.comma-separated-values-text", "public.plain-text", "public.text"),
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
                )
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/**
 * Reads the picked document as UTF-8 text inside the required security-scoped resource access
 * window (sandboxed file providers require this bracket around every read); never invokes
 * [onPicked] on cancel or a read failure, mirroring the Android actual's contract.
 */
@OptIn(ExperimentalForeignApi::class)
private class CsvDocumentPickerDelegate(
    private val onPicked: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val accessed = url.startAccessingSecurityScopedResource()
        try {
            val text = NSData.dataWithContentsOfURL(url)?.toByteArray()?.decodeToString() ?: return
            onPicked(text)
        } finally {
            if (accessed) url.stopAccessingSecurityScopedResource()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
