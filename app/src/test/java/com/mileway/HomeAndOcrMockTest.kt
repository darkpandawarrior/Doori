package com.mileway

import com.mileway.stub.DemoConfigManager
import com.mileway.stub.HomeMockData
import com.mileway.stub.OcrBatchStatus
import com.mileway.stub.OcrMockData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates the home/shell mocks, OCR mocks, and the demo plugin-config flags.
 */
class HomeAndOcrMockTest {

    // --- OCR receipt extraction ---

    @Test
    fun `receipt extraction is deterministic for the same seed`() {
        val first = OcrMockData.receiptExtractionFor("receipt_001.jpg")
        val second = OcrMockData.receiptExtractionFor("receipt_001.jpg")
        assertEquals(first, second, "same seed must produce an identical extraction")
    }

    @Test
    fun `receipt extraction is well-formed for arbitrary seeds`() {
        val seeds = (0 until 50).map { "IMG_$it.jpg" }
        val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
        seeds.forEach { seed ->
            val extraction = OcrMockData.receiptExtractionFor(seed)
            assertTrue(extraction.amount >= 100.0, "amount too low for $seed: ${extraction.amount}")
            assertTrue(extraction.amount < 1000.0, "amount too high for $seed: ${extraction.amount}")
            assertTrue(dateRegex.matches(extraction.date), "bad date for $seed: ${extraction.date}")
            assertTrue(extraction.merchant.isNotBlank(), "merchant must not be blank for $seed")
        }
    }

    @Test
    fun `receipt extraction varies across seeds`() {
        val merchants = (0 until 50).map { OcrMockData.receiptExtractionFor("file-$it.png").merchant }.toSet()
        assertTrue(merchants.size > 1, "extraction should rotate merchants across seeds")
    }

    // --- OCR batch status ---

    @Test
    fun `batch status is deterministic for the same seed`() {
        val seeds = (0 until 20).map { "batch-$it.jpg" }
        seeds.forEach { seed ->
            assertEquals(
                OcrMockData.batchStatusFor(seed),
                OcrMockData.batchStatusFor(seed),
                "same seed must produce the same status: $seed"
            )
        }
    }

    @Test
    fun `batch status covers every enum value across seeds`() {
        val observed = (0 until 200).map { OcrMockData.batchStatusFor("upload-$it.jpg") }.toSet()
        assertEquals(
            OcrBatchStatus.entries.toSet(), observed,
            "all four batch statuses must be reachable, got $observed"
        )
    }

    @Test
    fun `batch status is weighted towards success`() {
        val statuses = (0 until 200).map { OcrMockData.batchStatusFor("upload-$it.jpg") }
        val successCount = statuses.count { it == OcrBatchStatus.SUCCESS }
        assertTrue(successCount > statuses.size / 2, "SUCCESS should dominate, got $successCount/200")
    }

    // --- Odometer triple-reading mock ---

    @Test
    fun `server reading matches device reading when not a multiple of 7`() {
        listOf(1, 15, 100, 14001).forEach { reading ->
            assertEquals(reading, OcrMockData.serverReadingFor(reading), "no discrepancy expected for $reading")
        }
    }

    @Test
    fun `server reading injects fixed offset on multiples of 7`() {
        listOf(7, 14, 14000, 70).forEach { reading ->
            assertEquals(
                reading + OcrMockData.ODOMETER_DISCREPANCY_OFFSET,
                OcrMockData.serverReadingFor(reading),
                "discrepancy expected for $reading"
            )
        }
    }

    @Test
    fun `injected discrepancy always exceeds the threshold`() {
        assertTrue(OcrMockData.ODOMETER_DISCREPANCY_OFFSET > OcrMockData.ODOMETER_DISCREPANCY_THRESHOLD)
        val device = 14000 // multiple of 7 -> discrepancy path
        val server = OcrMockData.serverReadingFor(device)
        assertTrue(OcrMockData.isDiscrepancy(device, server), "discrepancy path must trip the threshold")
    }

    @Test
    fun `matching readings are not flagged as discrepancy`() {
        val device = 14001 // not a multiple of 7 -> clean path
        val server = OcrMockData.serverReadingFor(device)
        assertFalse(OcrMockData.isDiscrepancy(device, server))
        assertFalse(OcrMockData.isDiscrepancy(100, 100 + OcrMockData.ODOMETER_DISCREPANCY_THRESHOLD))
    }

    // --- Home/shell mocks ---

    @Test
    fun `at-a-glance counts are all positive`() {
        val glance = HomeMockData.atAGlance()
        assertTrue(glance.unreportedTransactions > 0)
        assertTrue(glance.upcomingTrips > 0)
        assertTrue(glance.pendingInvoices > 0)
        assertTrue(glance.pendingExpenses > 0)
        assertTrue(glance.vouchersToFile > 0)
    }

    @Test
    fun `action banner and carousel are well-formed`() {
        val banner = HomeMockData.actionRequiredBanner()
        assertTrue(banner.amountText.isNotBlank())
        assertTrue(banner.count > 0)
        assertTrue(banner.message.isNotBlank())

        val items = HomeMockData.carouselItems()
        assertTrue(items.isNotEmpty(), "carousel must not be empty")
        items.forEach { item ->
            assertTrue(item.title.isNotBlank())
            assertTrue(item.subtitle.isNotBlank())
            assertTrue(item.badge.isNotBlank())
        }

        assertTrue(HomeMockData.notificationCount() > 0)
    }

    @Test
    fun `auth tokens are obviously fake`() {
        val tokens = HomeMockData.authTokens()
        assertTrue(tokens.accessToken.startsWith("demo-"), "access token must be obviously fake")
        assertTrue(tokens.refreshToken.startsWith("demo-"), "refresh token must be obviously fake")
    }

    // --- Config flag defaults ---

    @Test
    fun `new plugin-config flags have expected defaults`() {
        val config = DemoConfigManager()
        assertTrue(config.isOfficeSelectionOnExpenseEnabled())
        assertTrue(config.isInterOfficeEnabled())
        assertTrue(config.isOdometerUploadEnabled(source = "start"))
        assertTrue(config.isOdometerUploadEnabled(source = "end"))
        assertFalse(config.isBranchCheckInRequired(), "branch check-in defaults off")
        assertEquals(10.0, config.getMaxDailyDistanceKm(), 0.001)
    }

    @Test
    fun `journey disclaimer is a short non-blank consent text`() {
        val config = DemoConfigManager()
        val disclaimer = config.getJourneyDisclaimer()
        assertNotNull(disclaimer, "demo ships with a disclaimer so the consent sheet shows")
        assertTrue(disclaimer.isNotBlank())
        assertTrue(disclaimer.length < 300, "disclaimer should stay short")
    }
}
