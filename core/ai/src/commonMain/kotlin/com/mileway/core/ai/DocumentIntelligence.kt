package com.mileway.core.ai

import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.result.getOrNull

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
    ): DocumentAnalysis {
        // textRecognizer must resolve before aiAnalyzer.extract now: the AI prompt folds in
        // PromptGuard-wrapped OCR text as grounding (see DocumentAiAnalyzer.extract's ocrText
        // param), so the two passes can no longer run concurrently the way they did before that
        // wiring existed. classifier/RawTextFieldExtractor are pure/near-instant, so this costs
        // one real round trip (OCR), not two.
        val rawText = textRecognizer.recognize(image)
        val heuristicDocType = classifier.classify(rawText)
        val textFields = RawTextFieldExtractor.extract(rawText)
        // aiAnalyzer.extract returns a typed AiResult (see DocumentAiAnalyzer) — a failure
        // (unsupported platform, model not resident, empty reply, ...) degrades to the same
        // "no AI extraction" null AnalysisCombiner already handles, same as before this seam
        // carried a reason at all.
        val aiExtraction =
            if (aiAnalyzer.isAvailable()) aiAnalyzer.extract(image, prompt, rawText).getOrNull() else null

        val combined =
            combiner.combine(
                aiExtraction = aiExtraction,
                heuristicDocType = heuristicDocType,
                rawText = rawText,
                textFields = textFields,
            )
        val duplicate = dedup.check(combined.fields, timestampMillis, dedupCandidates)

        return DocumentAnalysis(
            docType = combined.docType,
            fields = combined.fields,
            rawText = combined.rawText,
            duplicate = duplicate,
            overallConfidence = combined.overallConfidence,
            contributingSources = combined.contributingSources,
        )
    }
}
