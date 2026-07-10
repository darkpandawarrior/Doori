package com.mileway.core.forms.suggestions

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.forms.FieldId
import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormSchema
import com.mileway.core.forms.isFieldValueBlank

/**
 * Confidence bucket a [FieldSuggestion] falls into — drives the chip's visual weight and the
 * "accept all high-confidence" affordance in `FormFieldWithSuggestions`.
 */
enum class SuggestionConfidence { HIGH, MEDIUM, LOW }

private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.6f

/** Below this, an extracted value is too unreliable to surface as a chip at all. */
private const val MIN_SUGGESTION_CONFIDENCE = 0.3f

fun confidenceTierOf(confidence: Float): SuggestionConfidence =
    when {
        confidence >= HIGH_CONFIDENCE_THRESHOLD -> SuggestionConfidence.HIGH
        confidence >= MEDIUM_CONFIDENCE_THRESHOLD -> SuggestionConfidence.MEDIUM
        else -> SuggestionConfidence.LOW
    }

/** One accept/dismiss-able suggestion for a currently-empty form field, derived from a [DocumentAnalysis]. */
data class FieldSuggestion(
    val fieldKey: FieldId,
    val docField: DocField,
    val label: String,
    val value: FormFieldValue,
    val displayValue: String,
    val confidence: Float,
    val tier: SuggestionConfidence,
    val source: AnalyzerSource,
)

/**
 * DocField -> the [FormFieldType]s it can plausibly fill, plus fieldKey/label keyword hints used to
 * pick the right field when a schema has more than one field of a matching type (e.g. `baseAmount`
 * vs `totalAmount` are both NUMBER/CURRENCY). No keyword match -> no suggestion for that DocField,
 * rather than guessing at the wrong field.
 */
private data class DocFieldTarget(val types: Set<FormFieldType>, val keywords: List<String>)

private val DOC_FIELD_TARGETS: Map<DocField, DocFieldTarget> =
    mapOf(
        DocField.MERCHANT to DocFieldTarget(setOf(FormFieldType.TEXT, FormFieldType.TEXTAREA), listOf("merchant", "vendor", "payee")),
        DocField.TOTAL to DocFieldTarget(setOf(FormFieldType.CURRENCY, FormFieldType.NUMBER), listOf("total", "amount")),
        DocField.TAX to DocFieldTarget(setOf(FormFieldType.CURRENCY, FormFieldType.NUMBER), listOf("tax", "gst")),
        DocField.DATE to DocFieldTarget(setOf(FormFieldType.DATE), listOf("date")),
        DocField.INVOICE_NO to DocFieldTarget(setOf(FormFieldType.TEXT, FormFieldType.IRN), listOf("invoice", "receipt", "bill", "irn")),
        DocField.ODOMETER to DocFieldTarget(setOf(FormFieldType.NUMBER), listOf("odometer", "km", "mileage")),
        DocField.CATEGORY to DocFieldTarget(setOf(FormFieldType.SELECT, FormFieldType.MASTER), listOf("category")),
        DocField.CURRENCY to DocFieldTarget(setOf(FormFieldType.SELECT), listOf("currency")),
    )

/** Pure DocField -> schema field mapping: first (by [MockFormSchema.rank]) type+keyword match, else null. */
fun matchFieldFor(
    docField: DocField,
    schema: List<MockFormSchema>,
): MockFormSchema? {
    val target = DOC_FIELD_TARGETS[docField] ?: return null
    return schema
        .filter { it.type in target.types }
        .sortedBy { it.rank }
        .firstOrNull { field -> target.keywords.any { kw -> field.fieldKey.contains(kw, ignoreCase = true) || field.label.contains(kw, ignoreCase = true) } }
}

/** Strips currency symbols/commas an extractor commonly leaves in (e.g. `"₹1,234.50"` -> `1234.50`). */
private fun String.toCleanDouble(): Double? = filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()

/** Converts an extracted raw string into the [FormFieldValue] shape [fieldType]'s control expects. */
private fun toFormFieldValue(
    fieldType: FormFieldType,
    raw: String,
    currencyCode: String,
): FormFieldValue? =
    when (fieldType) {
        FormFieldType.TEXT, FormFieldType.TEXTAREA, FormFieldType.EMAIL -> FormFieldValue.Text(raw)
        FormFieldType.NUMBER -> raw.toCleanDouble()?.let { FormFieldValue.Number(it) }
        FormFieldType.CURRENCY -> raw.toCleanDouble()?.let { FormFieldValue.Currency(it, currencyCode) }
        // ponytail: assumes core:ai already normalizes DATE to ISO-8601 (matches FormFieldValue.Date's
        // contract); revisit if a raw unnormalized OCR date format shows up in practice.
        FormFieldType.DATE -> FormFieldValue.Date(raw)
        FormFieldType.SELECT, FormFieldType.MASTER, FormFieldType.IRN -> FormFieldValue.Select(raw)
        else -> null
    }

/**
 * The suggestion list for [schema] given the user's current [values] and a [DocumentAnalysis].
 * Empty when [analysis] has no fields (the AI-unavailable degrade contract), when a DocField has no
 * matching schema field, or when the matching field is already filled in. Pure — no I/O, safe to
 * unit test and to recompute on every keystroke (the Compose wrapper debounces the call site instead).
 */
fun fieldSuggestions(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
    analysis: DocumentAnalysis,
): List<FieldSuggestion> {
    if (analysis.fields.isEmpty()) return emptyList()
    val currencyCode = analysis.fields[DocField.CURRENCY]?.value ?: "INR"

    return analysis.fields.mapNotNull { (docField, extracted) ->
        if (extracted.confidence < MIN_SUGGESTION_CONFIDENCE) return@mapNotNull null
        val field = matchFieldFor(docField, schema) ?: return@mapNotNull null
        if (!isFieldValueBlank(values[field.fieldKey])) return@mapNotNull null
        val formValue = toFormFieldValue(field.type, extracted.value, currencyCode) ?: return@mapNotNull null
        FieldSuggestion(
            fieldKey = field.fieldKey,
            docField = docField,
            label = field.label,
            value = formValue,
            displayValue = extracted.value,
            confidence = extracted.confidence,
            tier = confidenceTierOf(extracted.confidence),
            source = extracted.source,
        )
    }
}

/** The subset `FormFieldWithSuggestions`'s "accept all" affordance fills in one tap. */
fun highConfidenceSuggestions(suggestions: List<FieldSuggestion>): List<FieldSuggestion> = suggestions.filter { it.tier == SuggestionConfidence.HIGH }
