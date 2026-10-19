package com.mileway.feature.logging.policy

import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.ai.model.LOW_CONFIDENCE_THRESHOLD
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.forms.parseMinorAmount
import com.mileway.core.network.model.PolicyViolation

/** Capture-time explanations only; OCR provenance is not stored on expenses or reports. */
object ExplainableViolation {
    /** Cites the scanned total only when it still drives this amount check. */
    fun message(
        violation: PolicyViolation,
        amountText: String,
        currencyCode: String,
        analysis: DocumentAnalysis?,
        perHeadAnchorMinor: Long? = null,
    ): String {
        if (violation.id !in AmountViolationIds) return violation.message
        val amountMinor = parseMinorAmount(amountText) ?: return violation.message
        val total = matchingTotal(amountMinor, currencyCode, analysis) ?: return violation.message
        if (violation.id == "EXPENSE_PER_HEAD_OVER_LIMIT" && perHeadAnchorMinor != null && perHeadAnchorMinor != amountMinor) {
            return violation.message
        }
        val percent = (total.confidence * ConfidencePercentScale).toInt()
        val confidence =
            if (total.confidence < LOW_CONFIDENCE_THRESHOLD) {
                "low confidence $percent%: check it"
            } else {
                "$percent% confidence"
            }
        val scannedCurrency = analysis?.fields?.get(DocField.CURRENCY)?.value.orEmpty()
        val money = formatMinorCurrency(amountMinor, scannedCurrency).trim()
        return "OCR read the total as $money, $confidence. ${violation.message}"
    }

    private fun matchingTotal(
        amountMinor: Long,
        currencyCode: String,
        analysis: DocumentAnalysis?,
    ): ExtractedValue? {
        val total = analysis?.fields?.get(DocField.TOTAL) ?: return null
        if (parseMinorAmount(normalizeTotal(total.value)) != amountMinor) return null
        val scannedCurrency = analysis.fields[DocField.CURRENCY]?.value
        if (scannedCurrency != null && scannedCurrency != currencyCode) return null
        if (!total.confidence.isFinite() || total.confidence !in 0f..1f) return null
        return total
    }

    private fun normalizeTotal(value: String): String {
        val trimmed = value.trim()
        return if (CommaDecimal.matches(trimmed)) trimmed.replace(',', '.') else trimmed
    }

    private const val ConfidencePercentScale = 100
    private val CommaDecimal = Regex("^\\d+,\\d{1,2}$")

    private val AmountViolationIds =
        setOf(
            "expense-amount-threshold",
            "expense-amount-hard-stop",
            "EXPENSE_PER_HEAD_OVER_LIMIT",
        )
}
