package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef

/**
 * On-device generative extraction (ML Kit GenAI Prompt API on Android, Apple Foundation Models on
 * iOS). Only available on capable hardware — [DocumentIntelligence] never blocks on it, and every
 * caller gets a full [com.mileway.core.ai.model.DocumentAnalysis] whether or not it ran.
 */
interface DocumentAiAnalyzer {
    /** Cheap, synchronous capability check (feature flag / device tier), not a network call. */
    fun isAvailable(): Boolean

    /** Null means "declined to extract" (unavailable, low confidence, or a parse failure) — never throws. */
    suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction?
}
