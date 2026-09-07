package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.result.AiResult

/**
 * On-device generative extraction (ML Kit GenAI Prompt API on Android, Apple Foundation Models on
 * iOS). Only available on capable hardware — [DocumentIntelligence] never blocks on it, and every
 * caller gets a full [com.mileway.core.ai.model.DocumentAnalysis] whether or not it ran.
 */
interface DocumentAiAnalyzer {
    /** Cheap, synchronous capability check (feature flag / device tier), not a network call. */
    fun isAvailable(): Boolean

    /**
     * Typed [AiResult] — a failure names why via [com.siddharth.kmp.result.AiFailure] instead of
     * collapsing to a bare null; never throws.
     *
     * @param ocrText Already-recognized text for this same [image] ([com.mileway.core.ai.TextRecognizer]'s
     * output, computed by [com.mileway.core.ai.DocumentIntelligence] before this call) — an actual may
     * fold it into the model prompt (guarded via [com.siddharth.kmp.result.PromptGuard], since it is
     * untrusted document content) as grounding, on top of or instead of the raw [image]. Defaults to
     * empty so an actual with nothing to add from it doesn't have to change.
     */
    suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
        ocrText: String = "",
    ): AiResult<AiExtraction>
}
