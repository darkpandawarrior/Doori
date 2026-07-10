package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.ExtractedValue

/** [AnalysisCombiner.combine] output before [DuplicateDetector] fills in the duplicate verdict. */
data class CombinedAnalysis(
    val docType: DocType,
    val fields: Map<DocField, ExtractedValue>,
    val rawText: String,
    val overallConfidence: Float,
    val contributingSources: Set<AnalyzerSource>,
)

/**
 * Confidence-weighted merge of the three analyzer tiers into one result. The highest-value unit
 * target in `core:ai` — this is the whole "graceful degradation" contract: callers never branch on
 * which tier ran, they just get a lower-confidence [CombinedAnalysis] when AI is absent.
 *
 * Precedence when two tiers extract the *same* [DocField] at the *same* confidence: AI wins over
 * the heuristic classifier, which wins over the raw-regex tier (declaration order of
 * [SOURCE_PRIORITY]).
 */
class AnalysisCombiner {
    /**
     * @param aiExtraction   [DocumentAiAnalyzer] output, or null when unavailable/declined.
     * @param heuristicDocType [HeuristicClassifier] output — always available, the degrade-path docType.
     * @param rawText        [TextRecognizer] output — always available.
     * @param heuristicFields structured fields a heuristic tier contributed, if any (none exist yet
     *                        in V25; the parameter exists so this merge is exercised/testable now and
     *                        a real source can plug in later without touching this function).
     * @param textFields     raw-regex fields from [RawTextFieldExtractor].
     */
    fun combine(
        aiExtraction: AiExtraction?,
        heuristicDocType: DocType,
        rawText: String,
        heuristicFields: Map<DocField, ExtractedValue> = emptyMap(),
        textFields: Map<DocField, ExtractedValue> = emptyMap(),
    ): CombinedAnalysis {
        val fields = mergeFields(aiExtraction?.fields.orEmpty(), heuristicFields, textFields)

        val aiDocType =
            aiExtraction?.docType?.takeIf { aiExtraction.confidence >= AI_CONFIDENT_THRESHOLD }
        val docType = aiDocType ?: heuristicDocType

        val contributingSources =
            buildSet {
                add(AnalyzerSource.TEXT_RECOGNITION)
                add(AnalyzerSource.HEURISTIC_CLASSIFIER)
                if (aiExtraction != null) add(AnalyzerSource.ON_DEVICE_AI)
            }

        val overallConfidence = if (fields.isEmpty()) 0f else fields.values.map { it.confidence }.average().toFloat()

        return CombinedAnalysis(
            docType = docType,
            fields = fields,
            rawText = rawText,
            overallConfidence = overallConfidence,
            contributingSources = contributingSources,
        )
    }

    /** Per field: highest confidence wins; ties broken by [SOURCE_PRIORITY]. */
    private fun mergeFields(vararg tiers: Map<DocField, ExtractedValue>): Map<DocField, ExtractedValue> {
        val byField = mutableMapOf<DocField, ExtractedValue>()
        for (tier in tiers) {
            for ((field, candidate) in tier) {
                val current = byField[field]
                if (current == null || isBetter(candidate, current)) {
                    byField[field] = candidate
                }
            }
        }
        return byField
    }

    private fun isBetter(
        candidate: ExtractedValue,
        current: ExtractedValue,
    ): Boolean {
        if (candidate.confidence != current.confidence) return candidate.confidence > current.confidence
        return SOURCE_PRIORITY.getValue(candidate.source) > SOURCE_PRIORITY.getValue(current.source)
    }

    private companion object {
        const val AI_CONFIDENT_THRESHOLD = 0.6f

        val SOURCE_PRIORITY =
            mapOf(
                AnalyzerSource.ON_DEVICE_AI to 3,
                AnalyzerSource.HEURISTIC_CLASSIFIER to 2,
                AnalyzerSource.TEXT_RECOGNITION to 1,
            )
    }
}
