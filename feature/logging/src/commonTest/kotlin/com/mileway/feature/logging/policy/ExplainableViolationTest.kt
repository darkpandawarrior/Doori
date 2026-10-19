package com.mileway.feature.logging.policy

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.network.model.PolicyViolation
import com.mileway.core.network.model.ViolationSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplainableViolationTest {
    private val violation =
        PolicyViolation(
            id = "expense-amount-threshold",
            title = "Expense amount requires manager approval",
            message = "Expenses of ₹5,000 or more require manager approval before reimbursement.",
            severity = ViolationSeverity.VIOLATION,
        )

    private fun analysis(
        total: String,
        confidence: Float,
        currency: String? = null,
    ) = DocumentAnalysis(
        docType = DocType.RECEIPT,
        fields =
            buildMap {
                put(DocField.TOTAL, ExtractedValue(total, confidence, AnalyzerSource.TEXT_RECOGNITION))
                currency?.let { put(DocField.CURRENCY, ExtractedValue(it, confidence, AnalyzerSource.TEXT_RECOGNITION)) }
            },
        rawText = "",
        duplicate = DuplicateVerdict.Unique,
        overallConfidence = confidence,
        contributingSources = setOf(AnalyzerSource.TEXT_RECOGNITION),
    )

    @Test
    fun unchangedOcrValueCitesFieldAndConfidence() {
        val message = ExplainableViolation.explain(violation, "5200.00", "INR", analysis("5,200.00", 0.92f))
        assertTrue(message.startsWith(violation.message))
        assertTrue(message.endsWith("OCR read the total as ${formatMinorCurrency(520_000, "INR")} at 92% confidence."))
    }

    @Test
    fun userEditedAmountMakesNoOcrClaim() {
        assertEquals(violation.message, ExplainableViolation.explain(violation, "5300", "INR", analysis("5,200.00", 0.92f)))
    }

    @Test
    fun differentCurrencyMakesNoOcrClaim() {
        val scan = analysis("5200.00", 0.92f, currency = "USD")
        assertEquals(violation.message, ExplainableViolation.explain(violation, "5200", "INR", scan))
    }

    @Test
    fun lowConfidenceSaysCheckIt() {
        val message = ExplainableViolation.explain(violation, "5200", "INR", analysis("5200", 0.41f))
        assertTrue(message.endsWith("OCR read the total as ${formatMinorCurrency(520_000, "INR")}, low confidence 41%: check it."))
    }

    @Test
    fun noScanKeepsOriginalMessage() {
        assertEquals(violation.message, ExplainableViolation.explain(violation, "5200", "INR", null))
    }

    @Test
    fun nonAmountViolationIsNotExplained() {
        val other = violation.copy(id = "FX_POLICY_SKIPPED")
        assertEquals(other.message, ExplainableViolation.explain(other, "5200", "INR", analysis("5200", 0.9f)))
    }

    @Test
    fun unparseableOcrTotalMakesNoClaim() {
        assertEquals(violation.message, ExplainableViolation.explain(violation, "5200", "INR", analysis("5.2k", 0.9f)))
    }
}
