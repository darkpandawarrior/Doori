package com.mileway.core.ai

import com.mileway.core.ai.model.DocType
import kotlin.test.Test
import kotlin.test.assertEquals

class KeywordHeuristicClassifierTest {
    @Test
    fun `receipt keywords classify as RECEIPT`() {
        val text = "Thank you for shopping\nSubtotal 10.00\nCash tendered 20.00\nChange due 10.00"

        assertEquals(DocType.RECEIPT, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `invoice keywords classify as INVOICE`() {
        val text = "TAX INVOICE\nInvoice No: 4821\nBill To: Acme Corp\nGSTIN: 22AAAAA0000A1Z5\nDue Date: 01-01-2026"

        assertEquals(DocType.INVOICE, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `odometer keywords classify as ODOMETER`() {
        val text = "Odometer reading\nTrip A 120\nTrip B 45\nMileage: 48213 km"

        assertEquals(DocType.ODOMETER, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `travel ticket keywords classify as TRAVEL_TICKET`() {
        val text = "Boarding Pass\nFlight AI-202\nPNR: XJ82K\nGate 12\nSeat 14A\nDeparture 10:45"

        assertEquals(DocType.TRAVEL_TICKET, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `id document keywords classify as ID_DOCUMENT`() {
        val text = "PASSPORT\nDate of Birth: 01 Jan 1990\nNational ID: 12345"

        assertEquals(DocType.ID_DOCUMENT, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `no keyword hits classify as OTHER`() {
        val text = "This is a plain note with no document keywords at all."

        assertEquals(DocType.OTHER, KeywordHeuristicClassifier.classify(text))
    }

    @Test
    fun `empty text classifies as OTHER`() {
        assertEquals(DocType.OTHER, KeywordHeuristicClassifier.classify(""))
    }

    @Test
    fun `classification is case insensitive`() {
        assertEquals(DocType.RECEIPT, KeywordHeuristicClassifier.classify("SUBTOTAL 5.00 CHANGE DUE 1.00"))
    }

    @Test
    fun `more matching keywords wins over fewer from another doc type`() {
        // Three receipt keywords vs one invoice keyword ("invoice" appears nowhere here).
        val text = "Subtotal 5.00, Cash tendered 10.00, Change due 5.00"

        assertEquals(DocType.RECEIPT, KeywordHeuristicClassifier.classify(text))
    }
}
