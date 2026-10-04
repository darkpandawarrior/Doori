package com.mileway

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import com.mileway.core.media.ocr.ReceiptDedupHasher
import com.mileway.core.media.ocr.sampleReceiptLuma
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ReceiptLumaSamplerTest {
    @Before
    fun setUp() {
        stopKoin()
        startKoin { androidContext(RuntimeEnvironment.getApplication()) }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `large local image is sampled and bounds decode does not return a false null`() = runTest {
        val file = File.createTempFile("receipt", ".png", RuntimeEnvironment.getApplication().cacheDir)
        val bitmap = Bitmap.createBitmap(1800, 1600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val luma = assertNotNull(sampleReceiptLuma(file.toURI().toString()))
            assertEquals(ReceiptDedupHasher.WIDTH * ReceiptDedupHasher.HEIGHT, luma.size)
            assertTrue(luma.all { it == 255 })
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    @Test
    fun `unreadable local images and remote URIs skip image comparison`() = runTest {
        assertNull(sampleReceiptLuma("file:///nonexistent/receipt.png"))
        assertNull(sampleReceiptLuma("https://example.invalid/receipt.png"))
    }
}
