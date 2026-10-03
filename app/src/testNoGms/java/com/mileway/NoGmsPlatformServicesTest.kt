package com.mileway

import android.content.Context
import com.mileway.core.ai.DocumentAiAnalyzer
import com.mileway.core.ai.TextRecognizer
import com.mileway.core.media.DocumentScanBackend
import com.mileway.feature.media.repository.UnavailableOcrMediaRepository
import com.mileway.feature.tracking.service.location.RealLocationSourceFactory
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoGmsPlatformServicesTest {
    @Test
    fun `FOSS binding uses plain GPS and reports unavailable OCR without inventing data`() =
        runBlocking {
            val app =
                koinApplication {
                    androidContext(mockk<Context>(relaxed = true))
                    modules(platformServicesKoinModule())
                }
            try {
                val koin = app.koin
                val factory = koin.get<RealLocationSourceFactory>()
                assertIs<PlainLocationTracker>(factory.create(forceGpsOnly = false, initialIntervalMs = 4000))
                assertIs<PlainLocationTracker>(factory.create(forceGpsOnly = true, initialIntervalMs = 4000))
                val recognizer = koin.get<TextRecognizer>()
                assertFalse(recognizer.isAvailable())
                assertTrue(recognizer.recognize("content://receipt").isEmpty())
                assertFalse(koin.get<DocumentAiAnalyzer>().isAvailable())
                assertNull(koin.getOrNull<DocumentScanBackend>())
                val result = UnavailableOcrMediaRepository().runOcr("content://receipt")
                assertNull(result.detectedOdometer)
                assertTrue(result.rawText.contains("not available in this build"))
            } finally {
                app.close()
            }
        }
}
