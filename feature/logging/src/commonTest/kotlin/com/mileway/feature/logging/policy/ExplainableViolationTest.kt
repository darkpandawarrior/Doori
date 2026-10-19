package com.mileway.feature.logging.policy

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.network.model.PolicyViolation
import kotlin.test.Test
import kotlin.test.assertEquals

class ExplainableViolationTest {
    private val violation = PolicyViolation(id = "expense-amount-threshold", message = "Manager approval required.")

    @Test
    fun unchangedTotalCitesFieldConfidenceAndFormattedMoney() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "INR")}, 92% confidence. ${violation.message}",
            ExplainableViolation.message(violation, "5200.00", "INR", scan()),
        )
    }

    @Test
    fun userEditedAmountKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.message(violation, "5300", "INR", scan()))
    }

    @Test
    fun lowConfidenceAsksUserToCheckTotal() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "INR")}, low confidence 41%: check it. ${violation.message}",
            ExplainableViolation.message(violation, "5200", "INR", scan(0.41f)),
        )
    }

    @Test
    fun thresholdIsNotLowConfidence() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "INR")}, 60% confidence. ${violation.message}",
            ExplainableViolation.message(violation, "5200", "INR", scan(0.6f)),
        )
    }

    @Test
    fun noScanKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.message(violation, "5200", "INR", null))
    }

    @Test
    fun missingTotalKeepsOriginalMessage() {
        val withoutTotal = scan().let { it.copy(fields = it.fields - DocField.TOTAL) }
        assertEquals(violation.message, ExplainableViolation.message(violation, "5200", "INR", withoutTotal))
    }

    @Test
    fun changedCurrencyKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.message(violation, "5200", "USD", scan()))
    }

    @Test
    fun unrelatedChecksNeverCiteReceiptTotal() {
        val unrelatedIds =
            listOf("MILEAGE_OVER_POLICY_RATE", "MILEAGE_RATE_CAPPED", "FX_POLICY_SKIPPED", "FX_APPROXIMATE", "EXPENSE_OVER_MAX", "RECEIPT_RECOMMENDED")
        for (id in unrelatedIds) {
            val unrelated = violation.copy(id = id)
            assertEquals(unrelated.message, ExplainableViolation.message(unrelated, "5200", "INR", scan()))
        }
    }

    @Test
    fun allAmountChecksCanCiteTotal() {
        for (id in listOf("expense-amount-hard-stop", "EXPENSE_PER_HEAD_OVER_LIMIT")) {
            assertEquals(
                "OCR read the total as ${formatMinorCurrency(520000, "INR")}, 92% confidence. ${violation.message}",
                ExplainableViolation.message(violation.copy(id = id), "5200", "INR", scan(), 520000),
            )
        }
    }

    @Test
    fun cardAnchoredPerHeadCheckDoesNotAttributeDifferentAmountToOcr() {
        val perHead = violation.copy(id = "EXPENSE_PER_HEAD_OVER_LIMIT")
        assertEquals(perHead.message, ExplainableViolation.message(perHead, "5200", "INR", scan(), 600000))
    }

    @Test
    fun invalidInputOrConfidenceKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.message(violation, "invalid", "INR", scan()))
        for (confidence in listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.1f, 1.1f)) {
            assertEquals(violation.message, ExplainableViolation.message(violation, "5200", "INR", scan(confidence)))
        }
    }

    @Test
    fun commaDecimalTotalMatchesExactMinorUnits() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(1250, "INR")}, 92% confidence. ${violation.message}",
            ExplainableViolation.message(violation, "12.50", "INR", scan(total = " 12,50 ")),
        )
    }

    @Test
    fun oneDecimalTotalMatchesExactMinorUnits() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "INR")}, 92% confidence. ${violation.message}",
            ExplainableViolation.message(violation, "5200", "INR", scan(total = "5200.0")),
        )
    }

    @Test
    fun differentCommaDecimalTotalKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.message(violation, "13.50", "INR", scan(total = "12,50")))
    }

    @Test
    fun groupedOrCurrencyPrefixedTotalsAreNotNormalized() {
        for (total in listOf("5,200.00", "5,200", "INR 5200", "₹5200")) {
            assertEquals(violation.message, ExplainableViolation.message(violation, "5200", "INR", scan(total = total)))
        }
    }

    @Test
    fun lowConfidencePercentNeverRoundsUpToThreshold() {
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "INR")}, low confidence 59%: check it. ${violation.message}",
            ExplainableViolation.message(violation, "5200", "INR", scan(0.596f)),
        )
    }

    @Test
    fun missingCurrencyDoesNotAttributeCurrencyToOcr() {
        val withoutCurrency = scan().let { it.copy(fields = it.fields - DocField.CURRENCY) }
        assertEquals(
            "OCR read the total as ${formatMinorCurrency(520000, "").trim()}, 92% confidence. ${violation.message}",
            ExplainableViolation.message(violation, "5200", "INR", withoutCurrency),
        )
    }

    private fun scan(
        confidence: Float = 0.92f,
        total: String = "5200",
    ) = DocumentAnalysis(
        docType = DocType.RECEIPT,
        fields =
            mapOf(
                DocField.TOTAL to ExtractedValue(total, confidence, AnalyzerSource.TEXT_RECOGNITION),
                DocField.CURRENCY to ExtractedValue("INR", 0.9f, AnalyzerSource.TEXT_RECOGNITION),
            ),
        rawText = "Total INR 5200",
        duplicate = DuplicateVerdict.Unique,
        overallConfidence = 0.99f,
        contributingSources = setOf(AnalyzerSource.TEXT_RECOGNITION),
    )
}
