package com.mileway.core.ai.model

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
