package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DocumentExtractionFields
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.ai.model.LOW_CONFIDENCE_THRESHOLD
import com.mileway.core.ai.model.lowConfidenceFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiExtractionConfidenceTest {
    private val combiner = AnalysisCombiner()

    @Test
    fun `each receipt field uses its own evidence`() {
        val mapped = DocumentExtractionMapper.toFields(
            DocumentExtractionFields(merchant = "Cafe Roma", date = "2026-10-05", total = "12.50", currency = "INR"),
        )
        val text = RawTextFieldExtractor.extract("TOTAL 12.50 TAX 1.25 USD 05/10/2026")
        val result = combiner.combine(
            AiExtraction(DocType.RECEIPT, mapped, "", 0.99f),
            DocType.RECEIPT,
            "Cafe Roma TOTAL 12.50 TAX 1.25 USD 05/10/2026",
            textFields = text,
        )
        val fields = result.fields
        assertEquals(setOf(DocField.MERCHANT, DocField.DATE, DocField.TOTAL, DocField.TAX, DocField.CURRENCY), fields.keys)
        assertTrue(fields.getValue(DocField.TOTAL).confidence > fields.getValue(DocField.MERCHANT).confidence)
        assertTrue(fields.getValue(DocField.MERCHANT).confidence > fields.getValue(DocField.DATE).confidence)
        assertTrue(fields.getValue(DocField.TAX).confidence < LOW_CONFIDENCE_THRESHOLD)
        assertTrue(fields.getValue(DocField.CURRENCY).confidence < LOW_CONFIDENCE_THRESHOLD)
        assertTrue(fields.values.none { it.confidence == 0.99f })
    }

    @Test
    fun `malformed total and impossible date remain below the review threshold`() {
        val fields = DocumentExtractionMapper.toFields(DocumentExtractionFields(total = "12.OO", date = "31/02/2026"))
        val result = combiner.combine(AiExtraction(DocType.RECEIPT, fields, "", 0.99f), DocType.RECEIPT, "12.OO 31/02/2026")
        assertTrue(result.fields.getValue(DocField.TOTAL).confidence < LOW_CONFIDENCE_THRESHOLD)
        assertTrue(result.fields.getValue(DocField.DATE).confidence < LOW_CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `valid supported fallback beats malformed high confidence AI`() {
        val ai = mapOf(DocField.TOTAL to ExtractedValue("12.OO", 0.99f, AnalyzerSource.ON_DEVICE_AI))
        val result = combiner.combine(
            AiExtraction(DocType.RECEIPT, ai, "", 0.99f), DocType.RECEIPT, "TOTAL 12.50",
            textFields = RawTextFieldExtractor.extract("TOTAL 12.50"),
        )
        assertEquals("12.50", result.fields.getValue(DocField.TOTAL).value)
    }

    @Test
    fun `low confidence fields returns exactly those below the threshold`() {
        val fields = mapOf(
            DocField.MERCHANT to ExtractedValue("Cafe", 0.9f, AnalyzerSource.ON_DEVICE_AI),
            DocField.DATE to ExtractedValue("bad date", 0.2f, AnalyzerSource.ON_DEVICE_AI),
            DocField.TOTAL to ExtractedValue("12.50", LOW_CONFIDENCE_THRESHOLD, AnalyzerSource.ON_DEVICE_AI),
            DocField.TAX to ExtractedValue("1.25", 0.4f, AnalyzerSource.TEXT_RECOGNITION),
            DocField.CURRENCY to ExtractedValue("INR", 0.55f, AnalyzerSource.ON_DEVICE_AI),
        )
        val analysis = DocumentAnalysis(DocType.RECEIPT, fields, "", DuplicateVerdict.Unique, 0.5f, emptySet())
        assertEquals(setOf(DocField.DATE, DocField.TAX, DocField.CURRENCY), analysis.lowConfidenceFields())
        assertEquals(setOf(DocField.DATE), analysis.lowConfidenceFields(0.3f))
    }

    @Test
    fun `leap dates are valid but non leap dates are not`() {
        assertTrue(FieldConfidence.valid(DocField.DATE, "2024-02-29"))
        assertTrue(!FieldConfidence.valid(DocField.DATE, "2026-02-29"))
    }
}
