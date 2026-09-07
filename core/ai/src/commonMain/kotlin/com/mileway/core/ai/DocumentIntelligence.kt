package com.mileway.core.ai

import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.result.getOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * The one public entry point every "OCR" caller in the app consumes (media capture, expense
 * scanner, form smart-suggest, the assistant). Produces a single [DocumentAnalysis] no matter
 * which analyzer tiers were available — see [AnalysisCombiner] for the degradation contract.
 */
class DocumentIntelligence(
    private val aiAnalyzer: DocumentAiAnalyzer,
    private val textRecognizer: TextRecognizer,
    private val classifier: HeuristicClassifier,
    private val combiner: AnalysisCombiner = AnalysisCombiner(),
    private val dedup: DuplicateDetector = DuplicateDetector(),
) {
    suspend fun analyze(
        image: DocumentImageRef,
        prompt: DocPrompt,
        dedupCandidates: List<DedupCandidate> = emptyList(),
        timestampMillis: Long = 0L,
    ): DocumentAnalysis =
        coroutineScope {
            // aiAnalyzer and textRecognizer are the two real I/O-bound passes — run them
            // concurrently. classifier is pure/near-instant but needs OCR text as input, so it
            // runs right after textRecognizer resolves, while aiAnalyzer may still be in flight.
            val aiDeferred = async { if (aiAnalyzer.isAvailable()) aiAnalyzer.extract(image, prompt) else null }
            val rawText = textRecognizer.recognize(image)
            val heuristicDocType = classifier.classify(rawText)
            val textFields = RawTextFieldExtractor.extract(rawText)
            // aiAnalyzer.extract returns a typed AiResult (see DocumentAiAnalyzer) — a failure
            // (unsupported platform, model not resident, empty reply, ...) degrades to the same
            // "no AI extraction" null AnalysisCombiner already handles, same as before this seam
            // carried a reason at all.
            val aiExtraction = aiDeferred.await()?.getOrNull()

            val combined =
                combiner.combine(
                    aiExtraction = aiExtraction,
                    heuristicDocType = heuristicDocType,
                    rawText = rawText,
                    textFields = textFields,
                )
            val duplicate = dedup.check(combined.fields, timestampMillis, dedupCandidates)

            DocumentAnalysis(
                docType = combined.docType,
                fields = combined.fields,
                rawText = combined.rawText,
                duplicate = duplicate,
                overallConfidence = combined.overallConfidence,
                contributingSources = combined.contributingSources,
            )
        }
}
