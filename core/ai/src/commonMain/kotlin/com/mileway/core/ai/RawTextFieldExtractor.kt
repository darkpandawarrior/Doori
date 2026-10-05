package com.mileway.core.ai

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.ExtractedValue

/**
 * Pure regex field extraction over OCR text — the "raw-regex" tier [AnalysisCombiner] ranks below
 * AI and the heuristic classifier. Deliberately narrow (amounts, date and currency): fields like MERCHANT
 * need real structure (AI or a schema-aware heuristic) to extract reliably from free text; adding
 * a guess here would just be a low-confidence source that always loses the combiner tie anyway.
 */
object RawTextFieldExtractor {
    private const val CONFIDENCE = 0.35f

    private val TOTAL_REGEX = Regex("""(?i)(?:total|amount)\D{0,10}(\d+[.,]\d{2})""")
    private val TAX_REGEX = Regex("""(?i)\btax\D{0,10}(\d+[.,]\d{2})""")
    private val CURRENCY_REGEX = Regex("""\b(INR|USD|EUR|GBP|CAD|AUD|JPY)\b""", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")

    fun extract(rawText: String): Map<DocField, ExtractedValue> {
        val fields = mutableMapOf<DocField, ExtractedValue>()
        TOTAL_REGEX.find(rawText)?.let {
            fields[DocField.TOTAL] = ExtractedValue(it.groupValues[1], CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        DATE_REGEX.find(rawText)?.let {
            fields[DocField.DATE] = ExtractedValue(it.groupValues[1], CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        TAX_REGEX.find(rawText)?.let {
            fields[DocField.TAX] = ExtractedValue(it.groupValues[1], CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        CURRENCY_REGEX.find(rawText)?.let {
            fields[DocField.CURRENCY] = ExtractedValue(it.value.uppercase(), CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        return fields
    }
}
