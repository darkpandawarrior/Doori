package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.ai.FoundationModelsOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result

/**
 * Real actual: kmp-toolkit's :ai [FoundationModelsOnDeviceLlm] (backed by its
 * `InjectableNativeLlm`/`FoundationModelsBridge` seam — a Swift class conforming to `NativeLlm`
 * registered at app startup, see `iosApp/iosApp/ai/FoundationModelsBridge.swift` and
 * `AppDelegate.swift`). Replaces the hand-copied `InjectableDocumentAiAnalyzer`/
 * `FoundationModelsBridge` this module used to carry — that generic delegate-or-degrade shape now
 * lives once in the toolkit instead of being re-copied per app. Mirrors [MlKitGenAiAnalyzer]'s
 * split: this class owns document-scan prompt building + JSON-field parsing (via
 * [DocFieldJsonParser], shared with the Android actual), not the model-client plumbing.
 *
 * ponytail: [FoundationModelsOnDeviceLlm]'s bridge is text-only — `NativeLlm.generate(prompt:
 * String)` carries no image parameter at all (Apple's on-device Foundation Models framework has no
 * vision input on this bridge), so [image] is accepted (interface parity with the Android
 * multimodal actual) but never read; only [prompt]'s instruction/schemaHint drive the model. This
 * matches the previous Swift stub's own behavior (it also never read the image), so no capability
 * regresses here. Upgrade if the toolkit bridge (or Apple's framework) ever offers an
 * image-carrying call.
 */
class FoundationModelsAnalyzer(
    private val llm: OnDeviceLlm = FoundationModelsOnDeviceLlm(),
) : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiResult<AiExtraction> {
        if (!isAvailable()) return Result.Failure(AiFailure.NotSupportedOnPlatform)
        return when (val textResult = llm.generate("${prompt.instruction}\n\n${prompt.schemaHint}")) {
            is Result.Failure -> textResult
            is Result.Success -> {
                val text = textResult.data
                if (text.isBlank()) {
                    Result.Failure(AiFailure.EmptyReply)
                } else {
                    Result.Success(
                        AiExtraction(
                            docType = DocFieldJsonParser.parseDocType(text),
                            fields = DocFieldJsonParser.parseFields(text),
                            rawText = text,
                            confidence = DocFieldJsonParser.RESPONSE_CONFIDENCE,
                        ),
                    )
                }
            }
        }
    }
}
