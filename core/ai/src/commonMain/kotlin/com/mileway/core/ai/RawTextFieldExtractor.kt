package com.mileway.core.ai

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.ExtractedValue

/**
 * Pure regex field extraction over OCR text — the "raw-regex" tier [AnalysisCombiner] ranks below
 * AI and the heuristic classifier. Deliberately narrow (TOTAL/DATE only): fields like MERCHANT
 * need real structure (AI or a schema-aware heuristic) to extract reliably from free text; adding
 * a guess here would just be a low-confidence source that always loses the combiner tie anyway.
 */
object RawTextFieldExtractor {
    // ponytail: fixed confidence for every regex hit — a real per-match confidence model isn't
    // worth it while this tier only exists as the combiner's fallback (V26 may need to revisit).
    private const val CONFIDENCE = 0.35f

    private val TOTAL_REGEX = Regex("""(?i)(?:total|amount)\D{0,10}(\d+[.,]\d{2})""")
    private val DATE_REGEX = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")

    fun extract(rawText: String): Map<DocField, ExtractedValue> {
        val fields = mutableMapOf<DocField, ExtractedValue>()
        TOTAL_REGEX.find(rawText)?.let {
            fields[DocField.TOTAL] = ExtractedValue(it.groupValues[1], CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        DATE_REGEX.find(rawText)?.let {
            fields[DocField.DATE] = ExtractedValue(it.groupValues[1], CONFIDENCE, AnalyzerSource.TEXT_RECOGNITION)
        }
        return fields
    }
}
