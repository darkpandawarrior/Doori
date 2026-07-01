package com.mileway

import com.mileway.feature.media.ocr.OdometerOcrParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [OdometerOcrParser].
 *
 * All tests run on the plain JVM, no Android device or Robolectric needed because
 * [OdometerOcrParser] contains no Android imports.
 */
class OdometerOcrParseTest {

    // -------------------------------------------------------------------------
    // Labelled-line hits (highest priority, highest confidence)
    // -------------------------------------------------------------------------

    @Test
    fun `extracts reading from labelled odometer line`() {
        val lines = listOf(
            "FUEL RECEIPT",
            "Station: Demo Petroleum #4821",
            "Odometer: 048213 km",
            "Litres: 41.20   Total: 73.18"
        )
        assertEquals("048213", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `extracts reading when label is odo abbreviation`() {
        val lines = listOf(
            "ODO 12345",
            "Trip A 0.0",
            "Trip B 0.0"
        )
        assertEquals("12345", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `extracts reading with km suffix on same line`() {
        val lines = listOf(
            "Current km: 87654",
            "Next service: 90000 km"
        )
        // "Current km: 87654" is a labelled line; "87654" is the first 5-digit group.
        assertEquals("87654", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `extracts reading with comma thousands separator`() {
        val lines = listOf("ODO 12,345 km")
        assertEquals("12345", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `extracts reading with miles label`() {
        val lines = listOf(
            "Mileage: 56789 miles"
        )
        assertEquals("56789", OdometerOcrParser.parse(lines))
    }

    // -------------------------------------------------------------------------
    // Unlabelled fallback (scan all non-noise lines)
    // -------------------------------------------------------------------------

    @Test
    fun `picks longest digit group from noisy multi-line receipt`() {
        val lines = listOf(
            "Trip A 123.4",
            "Reading 56789",
            "Date 2024-06-01",
            "Ref 8821"
        )
        // "Reading" is an ODO label; "56789" (5 digits) is found first in the labelled-line pass.
        // "8821" (4 digits) is also plausible but shorter.
        assertEquals("56789", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `returns null when all lines contain only noise`() {
        val lines = listOf(
            "Price: 73.18",
            "Tax: 4.50",
            "Date: 2024-06-01",
            "Total: 77.68"
        )
        // These all start with noise prefixes or have values < 4 digits; no valid odometer.
        // However "73.18" → "7318" is 4 digits so we need to ensure the noise filter works.
        // "Price" triggers noise filter; result should be null or any non-price candidate.
        // Since all are noise lines, null is expected.
        assertNull(OdometerOcrParser.parse(lines))
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(OdometerOcrParser.parse(emptyList()))
    }

    @Test
    fun `returns null when no digit group is 4 to 7 digits`() {
        val lines = listOf("AB CD EF", "123", "Hi there", "1 23 456")
        assertNull(OdometerOcrParser.parse(lines))
    }

    // -------------------------------------------------------------------------
    // OCR normalisation (common misread characters)
    // -------------------------------------------------------------------------

    @Test
    fun `normalises OCR misread O between digits`() {
        // "04821O" → "048210" after normalisation
        val lines = listOf("Odometer: 04821O km")
        val result = OdometerOcrParser.parse(lines)
        assertNotNull(result)
        assertEquals(6, result.length)
        assert(result.all { it.isDigit() }) { "Result must be all digits, was: $result" }
    }

    @Test
    fun `normalises I between digits to 1`() {
        val lines = listOf("km: 4812I km")
        val result = OdometerOcrParser.parse(lines)
        assertNotNull(result)
        assertEquals("48121", result)
    }

    // -------------------------------------------------------------------------
    // Reasonableness gate
    // -------------------------------------------------------------------------

    @Test
    fun `rejects 8-digit number as out of range`() {
        // 10_000_000 > MAX_READING (9_999_999) but is 8 digits so also fails length check.
        val lines = listOf("ODO 10000000 km")
        assertNull(OdometerOcrParser.parse(lines))
    }

    @Test
    fun `accepts maximum plausible reading 9999999`() {
        val lines = listOf("Odometer: 9999999 km")
        assertEquals("9999999", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `accepts minimum plausible reading of 4 digits`() {
        val lines = listOf("ODO 1234")
        assertEquals("1234", OdometerOcrParser.parse(lines))
    }

    // -------------------------------------------------------------------------
    // Real-world multi-line samples
    // -------------------------------------------------------------------------

    @Test
    fun `handles typical dashboard photo with multiple number groups`() {
        val lines = listOf(
            "SPEED",
            "60",
            "KM/H",
            "ODO",
            "123456",
            "FUEL"
        )
        // "ODO" line doesn't have a digit on the same line, but "123456" on the next line
        // should be found in the unlabelled fallback pass.
        assertEquals("123456", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `picks odometer over trip meter on same receipt`() {
        val lines = listOf(
            "Trip A 0123",
            "Odometer: 045678 km",
            "Trip B 0456"
        )
        // "Odometer" is a labelled keyword line, this should win over Trip readings.
        assertEquals("045678", OdometerOcrParser.parse(lines))
    }

    @Test
    fun `single clean reading line with no label is returned`() {
        val lines = listOf("048213")
        assertEquals("048213", OdometerOcrParser.parse(lines))
    }
}
