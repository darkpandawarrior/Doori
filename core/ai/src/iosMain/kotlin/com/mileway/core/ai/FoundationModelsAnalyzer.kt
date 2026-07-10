package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef

// ponytail: EXPERIMENTAL stub — real Apple Foundation Models (LanguageModelSession + @Generable)
// actual is V26 P-AI. Always reports unavailable so DocumentIntelligence exercises its degrade
// path on every iOS device today, including pre-Apple-Intelligence hardware.
class FoundationModelsAnalyzer : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = false

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? = null
}
