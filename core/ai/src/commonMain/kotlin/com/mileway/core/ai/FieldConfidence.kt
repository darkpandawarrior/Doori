package com.mileway.core.ai

import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.ExtractedValue

/** Conservative evidence scores for receipt review, not calibrated probabilities. */
internal object FieldConfidence {
    const val INVALID_CONFIDENCE = 0.2f
    private const val EVIDENCE_BONUS = 0.1f
    private const val UNGROUNDED_CONFIDENCE = 0.55f
    private const val ISO_YEAR_DIGITS = 4
    private const val SHORT_YEAR_LIMIT = 100
    private const val SHORT_YEAR_BASE = 2000
    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 9999
    private const val MONTHS_IN_YEAR = 12
    private const val LEAP_CYCLE = 4
    private const val CENTURY_CYCLE = 100
    private const val LEAP_CENTURY_CYCLE = 400
    private val receiptFields = setOf(DocField.MERCHANT, DocField.DATE, DocField.TOTAL, DocField.TAX, DocField.CURRENCY)
    private val amount = Regex("""\d+(?:[.,]\d{1,2})?""")
    private val date = Regex("""(\d{1,4})[/-](\d{1,2})[/-](\d{2,4})""")
    private val currency = Regex("[A-Z]{3}")

    fun valid(
        field: DocField,
        value: String,
    ): Boolean =
        when (field) {
            DocField.MERCHANT -> value.any { it.isLetter() }
            DocField.TOTAL, DocField.TAX -> amount.matches(value.trim())
            DocField.DATE -> validDate(value.trim())
            DocField.CURRENCY -> currency.matches(value.trim())
            else -> true
        }

    fun score(
        field: DocField,
        candidate: ExtractedValue,
        rawText: String,
        agreement: Boolean,
    ): Float {
        if (field !in receiptFields) return candidate.confidence
        if (!valid(field, candidate.value)) return minOf(candidate.confidence, INVALID_CONFIDENCE)
        val value = normalized(candidate.value)
        val supported = value.isNotEmpty() && Regex("(?<![A-Za-z0-9])${Regex.escape(value)}(?![A-Za-z0-9])").containsMatchIn(normalized(rawText))
        val grounded = if (supported) candidate.confidence + EVIDENCE_BONUS else minOf(candidate.confidence, UNGROUNDED_CONFIDENCE)
        return (grounded + if (agreement) EVIDENCE_BONUS else 0f).coerceIn(0f, 1f)
    }

    fun normalized(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun validDate(value: String): Boolean {
        val match = date.matchEntire(value) ?: return false
        val parts = match.groupValues.drop(1).map { it.toInt() }
        val iso = match.groupValues[1].length == ISO_YEAR_DIGITS
        val year = if (iso) parts[0] else parts[2].let { if (it < SHORT_YEAR_LIMIT) SHORT_YEAR_BASE + it else it }
        val month = parts[1]
        val day = if (iso) parts[2] else parts[0]
        if (year !in MIN_YEAR..MAX_YEAR || month !in 1..MONTHS_IN_YEAR) return false
        val leap = year % LEAP_CYCLE == 0 && (year % CENTURY_CYCLE != 0 || year % LEAP_CENTURY_CYCLE == 0)
        val days = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return day in 1..days[month - 1]
    }
}
