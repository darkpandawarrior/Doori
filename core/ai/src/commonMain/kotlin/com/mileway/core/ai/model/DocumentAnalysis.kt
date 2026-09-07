package com.mileway.core.ai.model

import kotlinx.serialization.Serializable

/**
 * Opaque handle to the source image passed into [com.mileway.core.ai.DocumentIntelligence.analyze].
 * A platform URI/path string today (mirrors `AttachmentItem.uri` in `core:media`) so commonMain and
 * commonTest stay platform-free; androidMain/iosMain actuals decode it into a real
 * bitmap/`CIImage` once the V26 platform work lands.
 */
typealias DocumentImageRef = String

/** One prompt template for a given [DocType], handed to [com.mileway.core.ai.DocumentAiAnalyzer]. */
data class DocPrompt(
    val docType: DocType,
    val instruction: String,
    val schemaHint: String,
)

/**
 * Schema-typed decode target for a [com.mileway.core.ai.DocumentAiAnalyzer] on-device reply, via
 * kmp-toolkit's `StructuredOutput<T>` — replaces the flat regex scrape (`DocFieldJsonParser`) both
 * platform actuals used to hand-roll. Deliberately the six fields named in the lane brief, not the
 * full [DocField] set (TAX/INVOICE_NO/CATEGORY stay heuristic-tier only for now — a narrower schema
 * is easier to get a small on-device model to fill in reliably; widen it once device output is
 * observed). All-nullable: a real reply omits whatever it didn't find rather than inventing a
 * value, and this also matches what [com.mileway.core.ai.DocumentExtractionMapper]'s tolerant
 * field-by-field mapping expects.
 */
@Serializable
data class DocumentExtractionFields(
    val docType: String? = null,
    val merchant: String? = null,
    val total: String? = null,
    val currency: String? = null,
    val date: String? = null,
    val odometer: String? = null,
)

/** Raw output of an on-device AI extraction pass, before [com.mileway.core.ai.AnalysisCombiner] merges it in. */
data class AiExtraction(
    val docType: DocType?,
    val fields: Map<DocField, ExtractedValue>,
    val rawText: String,
    val confidence: Float,
)

/**
 * The single combined result of [com.mileway.core.ai.DocumentIntelligence.analyze] — every caller
 * (media capture, expense scanner, form smart-suggest, the assistant) consumes this one shape
 * instead of branching on which analyzer tier ran.
 */
data class DocumentAnalysis(
    val docType: DocType,
    val fields: Map<DocField, ExtractedValue>,
    val rawText: String,
    val duplicate: DuplicateVerdict,
    val overallConfidence: Float,
    val contributingSources: Set<AnalyzerSource>,
)
