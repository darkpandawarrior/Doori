package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result

/** FOSS fallback: callers retain the image and offer manual fields. */
object NoTextRecognizer : TextRecognizer {
    override fun isAvailable(): Boolean = false

    override suspend fun recognize(image: DocumentImageRef): String = ""
}

/** No generative document extraction in builds without its platform backend. */
object NoDocumentAiAnalyzer : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = false

    override suspend fun extract(image: DocumentImageRef, prompt: DocPrompt, ocrText: String): AiResult<AiExtraction> =
        Result.Failure(AiFailure.NotSupportedOnPlatform)
}
