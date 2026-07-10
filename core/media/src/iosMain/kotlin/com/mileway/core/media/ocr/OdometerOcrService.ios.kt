package com.mileway.core.media.ocr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.FoundationModelsAnalyzer
import com.mileway.core.ai.KeywordHeuristicClassifier
import com.mileway.core.ai.VisionTextRecognizer

/**
 * iOS actual (V26 P26.CONV): [VisionTextRecognizer] is `core:ai`'s real Vision-backed recognizer —
 * unlike the old `feature:tracking`-only `VisionFrameTextRecognizer` stub it replaces, this one
 * actually runs `VNRecognizeTextRequest`. No real multi-pass image-enhancement pipeline exists on
 * iOS yet (Android's bitmap-variant technique is Android-graphics-specific), so gallery images fall
 * back to [SinglePassGalleryRecognizer] — still real Vision OCR, just not multi-pass-verified.
 */
@Composable
actual fun rememberOdometerOcrService(): OdometerOcrService =
    remember {
        val textRecognizer = VisionTextRecognizer()
        OdometerOcrService(
            textRecognizer = textRecognizer,
            galleryRecognizer = SinglePassGalleryRecognizer(textRecognizer),
            documentIntelligence =
                DocumentIntelligence(
                    aiAnalyzer = FoundationModelsAnalyzer(),
                    textRecognizer = textRecognizer,
                    classifier = KeywordHeuristicClassifier,
                ),
        )
    }
