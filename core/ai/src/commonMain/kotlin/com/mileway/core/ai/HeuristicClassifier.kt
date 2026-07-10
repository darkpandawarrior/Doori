package com.mileway.core.ai

import com.mileway.core.ai.model.DocType

/** Classifies recognized text into a [DocType] with no AI/network involved — the degrade-path fallback. */
interface HeuristicClassifier {
    fun classify(rawText: String): DocType
}

/**
 * Pure keyword-table classifier, the `core:ai` equivalent of `feature:media`'s `OdometerOcrParser`
 * idiom: no Android/ML Kit imports, directly unit-testable on the plain JVM.
 *
 * Scores each [DocType] by how many of its keywords appear in the (lowercased) text and returns
 * the best match, or [DocType.OTHER] when nothing scores above zero.
 */
object KeywordHeuristicClassifier : HeuristicClassifier {
    private val KEYWORDS: Map<DocType, Set<String>> =
        mapOf(
            DocType.RECEIPT to
                setOf("receipt", "subtotal", "cash tendered", "change due", "thank you for shopping", "qty"),
            DocType.INVOICE to
                setOf("invoice", "invoice no", "invoice number", "bill to", "due date", "gstin", "tax invoice"),
            DocType.ODOMETER to
                setOf("odometer", "odo", "mileage", "trip a", "trip b", "km reading"),
            DocType.TRAVEL_TICKET to
                setOf("boarding pass", "flight", "pnr", "seat", "gate", "itinerary", "e-ticket", "departure"),
            DocType.ID_DOCUMENT to
                setOf("passport", "driving licence", "driver's license", "date of birth", "national id", "aadhaar"),
        )

    override fun classify(rawText: String): DocType {
        val lower = rawText.lowercase()
        val best =
            KEYWORDS
                .mapValues { (_, keywords) -> keywords.count { lower.contains(it) } }
                .maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else DocType.OTHER
    }
}
