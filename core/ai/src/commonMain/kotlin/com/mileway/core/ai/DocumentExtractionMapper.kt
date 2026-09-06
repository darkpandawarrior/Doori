package com.mileway.core.ai

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentExtractionFields
import com.mileway.core.ai.model.ExtractedValue
import com.siddharth.kmp.result.PromptGuard

/**
 * Shared plumbing around kmp-toolkit's `StructuredOutput<DocumentExtractionFields>` decode — one
 * copy used by both [MlKitGenAiAnalyzer] (Android, multimodal) and [FoundationModelsAnalyzer] (iOS,
 * text-only), instead of the same field-mapping/prompt-guarding living twice per platform.
 *
 * Replaces the old flat "key": "value" regex scrape this module used to hand-roll (see git history
 * for `DocFieldJsonParser`) — `StructuredOutput` now owns the schema hint, tolerant parse and one
 * repair retry; this object only maps its typed decode result onto [DocField] and builds the
 * PromptGuard-wrapped instruction text both actuals send it.
 */
internal object DocumentExtractionMapper {
    // Above AnalysisCombiner.AI_CONFIDENT_THRESHOLD (0.6) so a confident docType/field call
    // actually wins the merge; still leaves room to tune once device output is observed.
    const val RESPONSE_CONFIDENCE = 0.7f

    fun toFields(fields: DocumentExtractionFields): Map<DocField, ExtractedValue> =
        buildMap {
            putField(DocField.MERCHANT, fields.merchant)
            putField(DocField.TOTAL, fields.total)
            putField(DocField.CURRENCY, fields.currency)
            putField(DocField.DATE, fields.date)
            putField(DocField.ODOMETER, fields.odometer)
        }

    fun toDocType(fields: DocumentExtractionFields): DocType? =
        fields.docType?.let { key -> DocType.entries.find { it.name.equals(key, ignoreCase = true) } }

    /**
     * [prompt]'s instruction+schemaHint, plus [ocrText] (already-recognized text — this never runs
     * OCR itself) appended when non-blank. [ocrText] is untrusted, physically-printed content (a
     * receipt can say anything), so it goes through [PromptGuard] before it reaches the model —
     * neutralizing "ignore previous instructions" printed on the document itself, not just typed
     * over chat.
     */
    fun buildInstruction(
        prompt: DocPrompt,
        ocrText: String,
    ): String {
        val base = "${prompt.instruction}\n\n${prompt.schemaHint}"
        if (ocrText.isBlank()) return base
        val guarded = PromptGuard.wrap(ocrText)
        return "$base\n\nOCR text recognized from the image:\n${guarded.text}"
    }

    private fun MutableMap<DocField, ExtractedValue>.putField(
        field: DocField,
        rawValue: String?,
    ) {
        if (rawValue.isNullOrBlank()) return
        this[field] = ExtractedValue(rawValue, RESPONSE_CONFIDENCE, AnalyzerSource.ON_DEVICE_AI)
    }
}
