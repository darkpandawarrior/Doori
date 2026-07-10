package com.mileway.core.ai.model

/** A single extracted field value plus provenance, so the combiner can rank competing candidates. */
data class ExtractedValue(
    val value: String,
    val confidence: Float,
    val source: AnalyzerSource,
)

/** Outcome of [com.mileway.core.ai.DuplicateDetector] checking a new document against history. */
sealed interface DuplicateVerdict {
    data object Unique : DuplicateVerdict

    data class Possible(val ref: String, val reason: String) : DuplicateVerdict

    data class Confirmed(val ref: String) : DuplicateVerdict
}

/**
 * A previously-recorded document [DuplicateDetector] can compare a new one against — deliberately
 * minimal (just what the merchant+amount+time heuristic needs), not a full expense/receipt model.
 */
data class DedupCandidate(
    val ref: String,
    val merchant: String?,
    val total: String?,
    val timestampMillis: Long,
)
