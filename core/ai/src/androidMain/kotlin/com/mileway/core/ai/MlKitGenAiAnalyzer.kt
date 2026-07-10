package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef

// ponytail: EXPERIMENTAL stub — real ML Kit GenAI Prompt API actual is V26 P-AI. Always reports
// unavailable so DocumentIntelligence exercises its degrade path on every Android device today.
class MlKitGenAiAnalyzer : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = false

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? = null
}
