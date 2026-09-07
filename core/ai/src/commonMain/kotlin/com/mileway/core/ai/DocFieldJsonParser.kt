package com.mileway.core.ai

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.ExtractedValue

/**
 * Shared best-effort JSON-field scrape for an on-device LLM's document-extraction reply — one copy
 * used by both [MlKitGenAiAnalyzer] (Android, multimodal) and [FoundationModelsAnalyzer] (iOS,
 * text-only), instead of the same regex living twice per platform.
 *
 * ponytail: flat "key": "value" regex scrape, not a schema-typed parser — no [com.mileway.core.ai.model.DocPrompt]
 * template ships a concrete JSON shape in this codebase today. Upgrade to kmp-toolkit's
 * `StructuredOutput<T>` once a schemaHint locks in a nested/typed output shape.
 */
internal object DocFieldJsonParser {
    // Above AnalysisCombiner.AI_CONFIDENT_THRESHOLD (0.6) so a confident docType/field call
    // actually wins the merge; still leaves room to tune once device output is observed.
    const val RESPONSE_CONFIDENCE = 0.7f

    private val jsonStringField = Regex(""""([A-Za-z_]+)"\s*:\s*"([^"]*)"""")
    private val docTypeField = Regex(""""docType"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

    fun parseFields(json: String): Map<DocField, ExtractedValue> {
        val fields = mutableMapOf<DocField, ExtractedValue>()
        for (match in jsonStringField.findAll(json)) {
            val (key, value) = match.destructured
            val field = DocField.entries.find { it.name.equals(key, ignoreCase = true) }
            // ktlint/detekt: single continue (LoopWithTooManyJumpStatements) — both "no field
            // matched" and "value blank" collapse to the same skip.
            if (value.isBlank() || field == null) continue
            fields[field] = ExtractedValue(value, RESPONSE_CONFIDENCE, AnalyzerSource.ON_DEVICE_AI)
        }
        return fields
    }

    fun parseDocType(json: String): DocType? {
        val key = docTypeField.find(json)?.groupValues?.get(1) ?: return null
        return DocType.entries.find { it.name.equals(key, ignoreCase = true) }
    }
}
