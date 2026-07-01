package com.mileway.core.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * iOS OCR via the Vision framework (F), the VNRecognizeTextRequest counterpart to Android's ML Kit
 * TextRecognizer. Wraps the image bytes in an NSData → UIImage → CGImage, runs an accurate-level text
 * request synchronously through a VNImageRequestHandler, and joins the top candidate of each observation.
 */
class IosTextRecognizer : TextRecognizer {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun recognize(imageBytes: ByteArray): String {
        if (imageBytes.isEmpty()) return ""
        val data =
            imageBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
            }
        val cgImage = UIImage(data = data).CGImage ?: return ""

        val request = VNRecognizeTextRequest(completionHandler = null)
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate

        val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())
        handler.performRequests(listOf(request), error = null)

        return request.results
            .orEmpty()
            .filterIsInstance<VNRecognizedTextObservation>()
            .mapNotNull { observation -> observation.topCandidates(1u).firstOrNull() as? platform.Vision.VNRecognizedText }
            .joinToString("\n") { it.string }
    }
}
