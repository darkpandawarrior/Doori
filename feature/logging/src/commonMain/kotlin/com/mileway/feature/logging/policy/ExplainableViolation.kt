package com.mileway.feature.logging.policy

import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.LOW_CONFIDENCE_THRESHOLD
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.forms.parseMinorAmount
import com.mileway.core.network.model.PolicyViolation
import kotlin.math.roundToInt

/**
 * Turns "flagged" into "flagged, and here is what the on-device scan read". Pure and offline: it only
 * reads the violation, the line's current field values and the optional [DocumentAnalysis] that the
 * receipt scan already produced. No model call, no network.
 *
 * Honesty rule: OCR is cited only while the amount field still holds the OCR value. A typed or edited
 * amount, a different currency, or no scan at all returns the original message unchanged.
 */
object ExplainableViolation {
    private const val PERCENT = 100

    /** Violation ids that are driven by the captured amount, the only field OCR can explain. */
    private val amountDriven =
        setOf("expense-amount-threshold", "expense-amount-hard-stop", "EXPENSE_PER_HEAD_OVER_LIMIT")

    fun explain(
        violation: PolicyViolation,
        amountText: String,
        currencyCode: String,
        analysis: DocumentAnalysis?,
    ): String {
        if (violation.id !in amountDriven) return violation.message
        val total = analysis?.fields?.get(DocField.TOTAL) ?: return violation.message
        val ocrCurrency = analysis.fields[DocField.CURRENCY]?.value?.trim()
        if (ocrCurrency != null && !ocrCurrency.equals(currencyCode, ignoreCase = true)) return violation.message
        val ocrMinor = parseOcrAmount(total.value) ?: return violation.message
        if (ocrMinor != parseMinorAmount(amountText)) return violation.message
        val read = formatMinorCurrency(ocrMinor, currencyCode)
        val percent = (total.confidence * PERCENT).roundToInt()
        val citation =
            if (total.confidence < LOW_CONFIDENCE_THRESHOLD) {
                "OCR read the total as $read, low confidence $percent%: check it."
            } else {
                "OCR read the total as $read at $percent% confidence."
            }
        return "${violation.message} $citation"
    }

    /** Applies [explain] to every violation, keeping ids and severities. */
    fun explainAll(
        violations: List<PolicyViolation>,
        amountText: String,
        currencyCode: String,
        analysis: DocumentAnalysis?,
    ): List<PolicyViolation> = violations.map { it.copy(message = explain(it, amountText, currencyCode, analysis)) }

    /** OCR text such as "5,200.00" or "5200" to minor units; anything else is not trusted. */
    private fun parseOcrAmount(raw: String): Long? = parseMinorAmount(raw.trim().replace(",", ""))
}
