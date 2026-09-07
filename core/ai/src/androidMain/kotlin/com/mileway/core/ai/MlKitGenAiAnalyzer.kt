package com.mileway.core.ai

import android.content.Context
import android.net.Uri
import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.ai.LlmPart
import com.siddharth.kmp.ai.MlKitGenAiOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result
import java.io.File

// ponytail: EXPERIMENTAL — delegates the ML Kit GenAI Prompt API call (Gemini Nano) to
// kmp-toolkit's :ai OnDeviceLlm seam (MlKitGenAiOnDeviceLlm) instead of re-deriving
// Generation.getClient()/FeatureStatus/GenerateContentRequest here (#11 consume) — this module
// now only owns document-scan prompt building + JSON-field parsing (see DocFieldJsonParser,
// shared with FoundationModelsAnalyzer/iOS), not the model-client plumbing. Compile-verified only,
// NOT device-verified: no Gemini-Nano-class hardware (Pixel 8+/AICore-eligible, locked bootloader)
// is available in this environment.
class MlKitGenAiAnalyzer(
    private val context: Context,
    private val llm: OnDeviceLlm = MlKitGenAiOnDeviceLlm(context),
) : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiResult<AiExtraction> {
        if (!isAvailable()) return Result.Failure(AiFailure.NotSupportedOnPlatform)
        return runCatching { runExtraction(image, prompt) }
            .getOrElse { Result.Failure(AiFailure.EmptyReply) }
    }

    private suspend fun runExtraction(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiResult<AiExtraction> {
        val bytes = readImageBytes(image) ?: return Result.Failure(AiFailure.EmptyReply)
        val parts = listOf(LlmPart.Image(bytes), LlmPart.Text("${prompt.instruction}\n\n${prompt.schemaHint}"))
        return when (val textResult = llm.generate(parts)) {
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

    private fun readImageBytes(uri: String): ByteArray? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { File(it).readBytes() } }.getOrNull()
    }
}
