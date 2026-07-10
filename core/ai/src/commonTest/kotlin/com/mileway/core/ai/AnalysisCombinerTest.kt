package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.ExtractedValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalysisCombinerTest {
    private val combiner = AnalysisCombiner()

    @Test
    fun `higher confidence source wins regardless of tier`() {
        val ai =
            AiExtraction(
                docType = DocType.RECEIPT,
                fields = mapOf(DocField.TOTAL to ExtractedValue("12.00", 0.4f, AnalyzerSource.ON_DEVICE_AI)),
                rawText = "text",
                confidence = 0.9f,
            )
        val text = mapOf(DocField.TOTAL to ExtractedValue("99.00", 0.9f, AnalyzerSource.TEXT_RECOGNITION))

        val result = combiner.combine(ai, DocType.OTHER, "text", textFields = text)

        assertEquals("99.00", result.fields.getValue(DocField.TOTAL).value)
    }

    @Test
    fun `tie is broken AI over heuristic over regex`() {
        val ai =
            AiExtraction(
                docType = DocType.RECEIPT,
                fields = mapOf(DocField.TOTAL to ExtractedValue("ai-value", 0.5f, AnalyzerSource.ON_DEVICE_AI)),
                rawText = "text",
                confidence = 0.9f,
            )
        val heuristic =
            mapOf(DocField.TOTAL to ExtractedValue("heuristic-value", 0.5f, AnalyzerSource.HEURISTIC_CLASSIFIER))
        val text = mapOf(DocField.TOTAL to ExtractedValue("regex-value", 0.5f, AnalyzerSource.TEXT_RECOGNITION))

        val result = combiner.combine(ai, DocType.OTHER, "text", heuristicFields = heuristic, textFields = text)

        assertEquals("ai-value", result.fields.getValue(DocField.TOTAL).value)
    }

    @Test
    fun `heuristic beats regex on tie when AI is absent`() {
        val heuristic =
            mapOf(DocField.TOTAL to ExtractedValue("heuristic-value", 0.5f, AnalyzerSource.HEURISTIC_CLASSIFIER))
        val text = mapOf(DocField.TOTAL to ExtractedValue("regex-value", 0.5f, AnalyzerSource.TEXT_RECOGNITION))

        val result = combiner.combine(null, DocType.OTHER, "text", heuristicFields = heuristic, textFields = text)

        assertEquals("heuristic-value", result.fields.getValue(DocField.TOTAL).value)
    }

    @Test
    fun `docType comes from confident AI classification`() {
        val ai = AiExtraction(DocType.INVOICE, emptyMap(), "text", confidence = 0.8f)

        val result = combiner.combine(ai, DocType.RECEIPT, "text")

        assertEquals(DocType.INVOICE, result.docType)
    }

    @Test
    fun `docType falls back to heuristic when AI is not confident`() {
        val ai = AiExtraction(DocType.INVOICE, emptyMap(), "text", confidence = 0.1f)

        val result = combiner.combine(ai, DocType.RECEIPT, "text")

        assertEquals(DocType.RECEIPT, result.docType)
    }

    @Test
    fun `contributingSources excludes ON_DEVICE_AI when AI did not run`() {
        val result = combiner.combine(null, DocType.OTHER, "text")

        assertFalse(AnalyzerSource.ON_DEVICE_AI in result.contributingSources)
        assertTrue(AnalyzerSource.TEXT_RECOGNITION in result.contributingSources)
        assertTrue(AnalyzerSource.HEURISTIC_CLASSIFIER in result.contributingSources)
    }

    @Test
    fun `contributingSources includes ON_DEVICE_AI when AI extraction is present`() {
        val ai = AiExtraction(DocType.RECEIPT, emptyMap(), "text", confidence = 0.9f)

        val result = combiner.combine(ai, DocType.OTHER, "text")

        assertTrue(AnalyzerSource.ON_DEVICE_AI in result.contributingSources)
    }

    @Test
    fun `overallConfidence is the average of the merged fields`() {
        val text =
            mapOf(
                DocField.TOTAL to ExtractedValue("1", 0.2f, AnalyzerSource.TEXT_RECOGNITION),
                DocField.DATE to ExtractedValue("2", 0.4f, AnalyzerSource.TEXT_RECOGNITION),
            )

        val result = combiner.combine(null, DocType.OTHER, "text", textFields = text)
        val expected = listOf(0.2f, 0.4f).map { it }.average().toFloat()

        assertEquals(expected, result.overallConfidence)
        assertTrue(result.overallConfidence in 0.29f..0.31f)
    }

    @Test
    fun `overallConfidence is zero when no fields were extracted`() {
        val result = combiner.combine(null, DocType.OTHER, "")

        assertEquals(0f, result.overallConfidence)
    }
}
