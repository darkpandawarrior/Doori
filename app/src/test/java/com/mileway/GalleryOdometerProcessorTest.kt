package com.mileway

import com.mileway.feature.media.ocr.GalleryOdometerProcessor
import com.mileway.feature.media.repository.FakeMediaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * D.2b: the gallery odometer processor parses the numeric reading + confidence out of the OCR result.
 * Uses the offline FakeMediaRepository (detectedOdometer "048213", confidence 0.93) so it runs on the JVM.
 */
class GalleryOdometerProcessorTest {
    @Test
    fun `parses odometer reading and confidence from gallery OCR`() =
        runTest {
            val processor = GalleryOdometerProcessor(FakeMediaRepository())
            val reading = processor.process("content://gallery/odo.jpg")
            assertEquals(48_213, reading.value)
            assertEquals(0.93f, reading.confidence, 0.0001f)
        }

    @Test
    fun `non-numeric OCR yields a null reading`() =
        runTest {
            val processor =
                GalleryOdometerProcessor(
                    object : com.mileway.feature.media.repository.MediaRepository by FakeMediaRepository() {
                        override suspend fun runOcr(uri: String) =
                            com.mileway.feature.media.model.OcrResult(
                                rawText = "no digits here",
                                detectedOdometer = null,
                                confidence = 0f,
                                watermarkApplied = false,
                            )
                    },
                )
            assertNull(processor.process("content://gallery/blank.jpg").value)
        }
}
