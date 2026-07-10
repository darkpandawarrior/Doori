package com.mileway.core.ai

import com.mileway.core.ai.model.DocumentImageRef
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * iOS [TextRecognizer] actual (V26 P26.AI.3) via the Vision framework's `VNRecognizeTextRequest`,
 * operating on `core:ai`'s [DocumentImageRef] (a path/`file://` URI string): load the file, decode
 * to `UIImage`/`CGImage`, run an accurate-level text request synchronously, join the top candidate
 * of each observation.
 *
 * Never throws — any decode or recognition failure resolves to "", which [DocumentIntelligence]
 * treats as "nothing to contribute" rather than a crash (mirrors [MlKitTextRecognizer]'s contract).
 */
class VisionTextRecognizer : TextRecognizer {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun recognize(image: DocumentImageRef): String =
        runCatching {
            val data = loadData(image) ?: return@runCatching ""
            val cgImage = UIImage(data = data).CGImage ?: return@runCatching ""

            val request = VNRecognizeTextRequest(completionHandler = null)
            request.recognitionLevel = VNRequestTextRecognitionLevelAccurate

            val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())
            handler.performRequests(listOf(request), error = null)

            request.results
                .orEmpty()
                .filterIsInstance<VNRecognizedTextObservation>()
                .mapNotNull { it.topCandidates(1u).firstOrNull() as? VNRecognizedText }
                .joinToString("\n") { it.string }
        }.getOrDefault("")

    /** [path] may be a `file://` URI (from `core:media`'s AttachmentItem.uri) or a plain path. */
    private fun loadData(path: String): NSData? {
        val url = if (path.startsWith("file:")) NSURL(string = path) else NSURL(fileURLWithPath = path)
        return NSData.dataWithContentsOfURL(url)
    }
}
