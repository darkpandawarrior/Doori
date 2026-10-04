package com.mileway.feature.cards

import com.mileway.feature.cards.import.StatementParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StatementParserTest {
    @Test
    fun csvFixtureReadsQuotedMerchantAndForeignMoneyExactly() {
        val text =
            "id,date,merchant,amount,currency,foreign_amount,foreign_currency\n" +
                "tx1,2026-09-25,\"Cafe, \"\"Orchard\"\"\",850.01,INR,10.00,USD"
        val parsed = StatementParser.parse(text)
        assertEquals("CSV", parsed.source)
        assertEquals("Cafe, \"Orchard\"", parsed.rows.single().merchant)
        assertEquals(85001, parsed.rows.single().amountMinor)
        assertEquals(1000, parsed.rows.single().foreignAmountMinor)
    }

    @Test
    fun ofxFixturesReadXmlAndSgmlAndIgnoreCredits() {
        for (xml in listOf(false, true)) {
            fun tag(
                name: String,
                value: String,
            ) = "<$name>$value" + if (xml) "</$name>" else "\n"
            val fixture =
                "<OFX><CURDEF>INR" + (if (xml) "</CURDEF>" else "\n") + tag("ACCTID", "fixture-card") +
                    "<STMTTRN>" + tag("TRNTYPE", "DEBIT") + tag("DTPOSTED", "20260925120000[0:GMT]") + tag("TRNAMT", "-120.00") +
                    tag("FITID", "fixture-1") + tag("NAME", "Cafe &amp; Orchard") + "</STMTTRN>" +
                    "<STMTTRN>" + tag("TRNTYPE", "CREDIT") + tag("TRNAMT", "120.00") + "</STMTTRN></OFX>"
            val parsed = StatementParser.parse(fixture)
            assertEquals("OFX", parsed.source)
            assertEquals("2026-09-25", parsed.rows.single().postingDate)
            assertEquals("Cafe & Orchard", parsed.rows.single().merchant)
            assertEquals(12000, parsed.rows.single().amountMinor)
        }
    }

    @Test
    fun invalidMoneyDateQuoteAndDuplicateIdRejectTheWholeFile() {
        val header = "id,date,merchant,amount,currency\n"
        for (body in listOf(
            "x,2026-02-30,Cafe,12.00,INR",
            "x,2026-09-25,Cafe,12.001,INR",
            "x,2026-09-25,\"Cafe,12.00,INR",
            "x,2026-09-25,Cafe,12.00,INR\nx,2026-09-25,Cafe,12.00,INR",
        )) {
            assertFailsWith<IllegalArgumentException> { StatementParser.parse(header + body) }
        }
    }
}
