package com.mileway.core.media.ocr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mileway.core.ai.NoTextRecognizer
import com.mileway.core.ai.TextRecognizer
import com.mileway.core.media.androidDocumentIntelligence
import org.koin.mp.KoinPlatform

@Composable
actual fun rememberOdometerOcrService(): OdometerOcrService =
    remember {
        val koin = KoinPlatform.getKoin()
        val textRecognizer = koin.getOrNull<TextRecognizer>() ?: NoTextRecognizer
        OdometerOcrService(
            textRecognizer = textRecognizer,
            galleryRecognizer = koin.getOrNull<GalleryMultiPassRecognizer>() ?: SinglePassGalleryRecognizer(textRecognizer),
            documentIntelligence = androidDocumentIntelligence(),
        )
    }
