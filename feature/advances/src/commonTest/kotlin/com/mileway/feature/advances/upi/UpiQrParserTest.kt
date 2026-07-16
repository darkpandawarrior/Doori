package com.mileway.feature.advances.upi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpiQrParserTest {
    @Test
    fun `parses a upi pay link with all fields`() {
        val link = "upi://pay?pa=merchant@upi&pn=Merchant%20One&mc=1234&am=250.50&tr=TXN123&tn=Order%20note"

        val result = UpiQrParser.parse(link)

        assertEquals(
            UpiPayment(pa = "merchant@upi", pn = "Merchant One", mc = "1234", am = 250.50, tr = "TXN123", tn = "Order note"),
            result,
        )
    }

    @Test
    fun `upi link with only pa leaves the rest null`() {
        val result = UpiQrParser.parse("upi://pay?pa=merchant@upi")

        assertEquals(UpiPayment(pa = "merchant@upi"), result)
    }

    @Test
    fun `upi link missing pa returns null`() {
        val result = UpiQrParser.parse("upi://pay?pn=Merchant&am=100")

        assertNull(result)
    }

    @Test
    fun `upi link with no query string returns null`() {
        assertNull(UpiQrParser.parse("upi://pay"))
    }

    @Test
    fun `parses an EMV TLV payload with amount`() {
        val emv = tlv("26", tlv("01", "merchant@upi")) + tlv("59", "Merchant One") + tlv("52", "1234") + tlv("54", "99.99")

        val result = UpiQrParser.parse(emv)

        assertEquals(
            UpiPayment(pa = "merchant@upi", pn = "Merchant One", mc = "1234", am = 99.99, tr = null, tn = null),
            result,
        )
    }

    @Test
    fun `EMV tag 62 subtag 05 fills both tr and tn`() {
        val emv = tlv("26", tlv("01", "merchant@upi")) + tlv("62", tlv("05", "REF-99"))

        val result = UpiQrParser.parse(emv)

        assertEquals("REF-99", result?.tr)
        assertEquals("REF-99", result?.tn)
    }

    @Test
    fun `EMV payload without amount leaves am null`() {
        val emv = tlv("26", tlv("01", "merchant@upi"))

        val result = UpiQrParser.parse(emv)

        assertEquals(UpiPayment(pa = "merchant@upi"), result)
    }

    @Test
    fun `EMV payload missing tag 26 returns null`() {
        val emv = tlv("59", "Merchant One") + tlv("54", "10.00")

        assertNull(UpiQrParser.parse(emv))
    }

    @Test
    fun `garbage input returns null without throwing`() {
        assertNull(UpiQrParser.parse("not a upi payload at all"))
    }

    /** 2-digit-tag + 2-digit-decimal-length EMV TLV encoding, matching UpiQrParser's parseTlv. */
    private fun tlv(
        tag: String,
        value: String,
    ): String = tag + value.length.toString().padStart(2, '0') + value
}
