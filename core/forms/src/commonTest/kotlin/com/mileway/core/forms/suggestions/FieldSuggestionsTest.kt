package com.mileway.core.forms.suggestions

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun field(
    key: String,
    label: String,
    type: FormFieldType,
    rank: Int = 0,
) = MockFormSchema(id = key, fieldKey = key, label = label, type = type, rank = rank)

private fun analysis(
    fields: Map<DocField, ExtractedValue>,
    docType: DocType = DocType.RECEIPT,
) = DocumentAnalysis(
    docType = docType,
    fields = fields,
    rawText = "",
    duplicate = DuplicateVerdict.Unique,
    overallConfidence = fields.values.maxOfOrNull { it.confidence } ?: 0f,
    contributingSources = fields.values.map { it.source }.toSet(),
)

private fun extracted(
    value: String,
    confidence: Float = 0.9f,
    source: AnalyzerSource = AnalyzerSource.ON_DEVICE_AI,
) = ExtractedValue(value, confidence, source)

class FieldSuggestionsTest {
    // ---- degrade contract ----

    @Test
    fun emptyAnalysisFields_producesNoSuggestions() {
        val schema = listOf(field("merchant", "Merchant", FormFieldType.TEXT))
        val result = fieldSuggestions(schema, emptyMap(), analysis(emptyMap()))
        assertTrue(result.isEmpty())
    }

    // ---- DocField -> schema field mapping ----

    @Test
    fun matchFieldFor_picksFieldByTypeAndKeyword() {
        val schema =
            listOf(
                field("merchantName", "Merchant Name", FormFieldType.TEXT),
                field("notes", "Notes", FormFieldType.TEXTAREA),
            )
        assertEquals("merchantName", matchFieldFor(DocField.MERCHANT, schema)?.fieldKey)
    }

    @Test
    fun matchFieldFor_noKeywordMatch_returnsNull() {
        val schema = listOf(field("notes", "Notes", FormFieldType.TEXTAREA))
        assertNull(matchFieldFor(DocField.MERCHANT, schema))
    }

    @Test
    fun matchFieldFor_wrongType_returnsNull() {
        // "total" keyword present but the field is a DATE, not CURRENCY/NUMBER.
        val schema = listOf(field("totalDate", "Total Date", FormFieldType.DATE))
        assertNull(matchFieldFor(DocField.TOTAL, schema))
    }

    // ---- fieldSuggestions: end-to-end mapping + value conversion ----

    @Test
    fun total_mapsToCurrencyField_withDocCurrencyCode() {
        val schema = listOf(field("totalAmount", "Total", FormFieldType.CURRENCY))
        val docAnalysis =
            analysis(
                mapOf(
                    DocField.TOTAL to extracted("₹1,234.50"),
                    DocField.CURRENCY to extracted("USD"),
                ),
            )
        val result = fieldSuggestions(schema, emptyMap(), docAnalysis)
        val suggestion = result.single { it.docField == DocField.TOTAL }
        assertEquals(FormFieldValue.Currency(1234.50, "USD"), suggestion.value)
    }

    @Test
    fun date_passesThroughVerbatim() {
        val schema = listOf(field("tripDate", "Trip Date", FormFieldType.DATE))
        val docAnalysis = analysis(mapOf(DocField.DATE to extracted("2026-07-10")))
        val suggestion = fieldSuggestions(schema, emptyMap(), docAnalysis).single()
        assertEquals(FormFieldValue.Date("2026-07-10"), suggestion.value)
    }

    @Test
    fun alreadyFilledField_isExcluded() {
        val schema = listOf(field("merchantName", "Merchant Name", FormFieldType.TEXT))
        val docAnalysis = analysis(mapOf(DocField.MERCHANT to extracted("Acme")))
        val result = fieldSuggestions(schema, mapOf("merchantName" to FormFieldValue.Text("Already filled")), docAnalysis)
        assertTrue(result.isEmpty())
    }

    @Test
    fun lowConfidenceExtraction_isExcluded() {
        val schema = listOf(field("merchantName", "Merchant Name", FormFieldType.TEXT))
        val docAnalysis = analysis(mapOf(DocField.MERCHANT to extracted("Acme", confidence = 0.1f)))
        assertTrue(fieldSuggestions(schema, emptyMap(), docAnalysis).isEmpty())
    }

    @Test
    fun unmatchedDocField_isSkippedWithoutCrashing() {
        val schema = listOf(field("notes", "Notes", FormFieldType.TEXTAREA))
        val docAnalysis = analysis(mapOf(DocField.ODOMETER to extracted("12345")))
        assertTrue(fieldSuggestions(schema, emptyMap(), docAnalysis).isEmpty())
    }

    // ---- confidence tiering ----

    @Test
    fun confidenceTierOf_boundaries() {
        assertEquals(SuggestionConfidence.HIGH, confidenceTierOf(0.85f))
        assertEquals(SuggestionConfidence.HIGH, confidenceTierOf(0.99f))
        assertEquals(SuggestionConfidence.MEDIUM, confidenceTierOf(0.6f))
        assertEquals(SuggestionConfidence.MEDIUM, confidenceTierOf(0.84f))
        assertEquals(SuggestionConfidence.LOW, confidenceTierOf(0.59f))
        assertEquals(SuggestionConfidence.LOW, confidenceTierOf(0.3f))
    }

    @Test
    fun highConfidenceSuggestions_filtersToHighTierOnly() {
        val schema =
            listOf(
                field("merchantName", "Merchant Name", FormFieldType.TEXT),
                field("odometer", "Odometer", FormFieldType.NUMBER),
            )
        val docAnalysis =
            analysis(
                mapOf(
                    DocField.MERCHANT to extracted("Acme", confidence = 0.95f),
                    DocField.ODOMETER to extracted("12345", confidence = 0.5f),
                ),
            )
        val all = fieldSuggestions(schema, emptyMap(), docAnalysis)
        val high = highConfidenceSuggestions(all)
        assertEquals(1, high.size)
        assertEquals(DocField.MERCHANT, high.single().docField)
    }
}
