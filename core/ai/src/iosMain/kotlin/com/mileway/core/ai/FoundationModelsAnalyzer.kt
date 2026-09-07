package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentExtractionFields
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.ai.FoundationModelsOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import com.siddharth.kmp.ai.structuredOutput
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result

/**
 * Real actual: kmp-toolkit's :ai [FoundationModelsOnDeviceLlm] (backed by its
 * `InjectableNativeLlm`/`FoundationModelsBridge` seam — a Swift class conforming to `NativeLlm`
 * registered at app startup, see `iosApp/iosApp/ai/FoundationModelsBridge.swift` and
 * `AppDelegate.swift`). Mirrors [MlKitGenAiAnalyzer]'s split: this class owns document-scan prompt
 * building (via [DocumentExtractionMapper], shared with the Android actual), not the model-client
 * plumbing or the StructuredOutput<DocumentExtractionFields> reply parse.
 *
 * ponytail: [FoundationModelsOnDeviceLlm]'s bridge is text-only — `NativeLlm.generate(prompt:
 * String)` carries no image parameter at all (Apple's on-device Foundation Models framework has no
 * vision input on this bridge), so [image] is accepted (interface parity with the Android
 * multimodal actual) but never read. [ocrText] is what actually grounds this tier now — before
 * DocumentAiAnalyzer.extract carried it, this analyzer ran on instruction+schemaHint alone with no
 * document content at all; upgrade [image] to a real vision call if the toolkit bridge (or Apple's
 * framework) ever offers one.
 */
class FoundationModelsAnalyzer(
    private val llm: OnDeviceLlm = FoundationModelsOnDeviceLlm(),
) : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
        ocrText: String,
    ): AiResult<AiExtraction> {
        if (!isAvailable()) return Result.Failure(AiFailure.NotSupportedOnPlatform)
        val instruction = DocumentExtractionMapper.buildInstruction(prompt, ocrText)
        val result =
            structuredOutput<DocumentExtractionFields>().ask(instruction) { hintedPrompt -> llm.generate(hintedPrompt) }
        return when (result) {
            is Result.Failure -> result
            is Result.Success -> {
                val fields = result.data
                Result.Success(
                    AiExtraction(
                        docType = DocumentExtractionMapper.toDocType(fields),
                        fields = DocumentExtractionMapper.toFields(fields),
                        rawText = fields.toString(),
                        confidence = DocumentExtractionMapper.RESPONSE_CONFIDENCE,
                    ),
                )
            }
        }
    }
}
