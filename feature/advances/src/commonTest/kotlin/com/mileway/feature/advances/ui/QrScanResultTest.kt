package com.mileway.feature.advances.ui

import com.mileway.feature.advances.ui.components.QrScanResult
import com.mileway.feature.advances.ui.components.parseScanResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrScanResultTest {
    @Test
    fun `a valid UPI deep link parses into a Parsed result with the payee vpa`() {
        val result = parseScanResult("upi://pay?pa=payee@bank&pn=Payee%20Name&am=250.0")

        val parsed = assertIs<QrScanResult.Parsed>(result)
        assertEquals("payee@bank", parsed.payment.pa)
        assertEquals("Payee Name", parsed.payment.pn)
        assertEquals(250.0, parsed.payment.am)
    }

    @Test
    fun `text that is neither a UPI link nor EMV TLV is a ParseError`() {
        val result = parseScanResult("not a qr payload at all")

        assertEquals(QrScanResult.ParseError, result)
    }
}
