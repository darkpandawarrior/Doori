package com.mileway.core.ai

import android.content.Context
import android.net.Uri
import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentExtractionFields
import com.mileway.core.ai.model.DocumentImageRef
import com.siddharth.kmp.ai.LlmPart
import com.siddharth.kmp.ai.MlKitGenAiOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import com.siddharth.kmp.ai.structuredOutput
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result
import java.io.File

// ponytail: EXPERIMENTAL — delegates the ML Kit GenAI Prompt API call (Gemini Nano) to
// kmp-toolkit's :ai OnDeviceLlm seam (MlKitGenAiOnDeviceLlm) instead of re-deriving
// Generation.getClient()/FeatureStatus/GenerateContentRequest here (#11 consume), and the reply
// parse to kmp-toolkit's StructuredOutput<DocumentExtractionFields> instead of a hand-rolled regex
// scrape (#document-ocr-ai-extraction consume) — this module now only owns document-scan prompt
// building (DocumentExtractionMapper, shared with FoundationModelsAnalyzer/iOS), not the
// model-client plumbing or the reply parsing. Compile-verified only, NOT device-verified: no
// Gemini-Nano-class hardware (Pixel 8+/AICore-eligible, locked bootloader) is available in this
// environment.
class MlKitGenAiAnalyzer(
    private val context: Context,
    private val llm: OnDeviceLlm = MlKitGenAiOnDeviceLlm(context),
) : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
        ocrText: String,
    ): AiResult<AiExtraction> {
        if (!isAvailable()) return Result.Failure(AiFailure.NotSupportedOnPlatform)
        return runCatching { runExtraction(image, prompt, ocrText) }
            .getOrElse { Result.Failure(AiFailure.EmptyReply) }
    }

    private suspend fun runExtraction(
        image: DocumentImageRef,
        prompt: DocPrompt,
        ocrText: String,
    ): AiResult<AiExtraction> {
        val bytes = readImageBytes(image) ?: return Result.Failure(AiFailure.EmptyReply)
        val instruction = DocumentExtractionMapper.buildInstruction(prompt, ocrText)
        val result =
            structuredOutput<DocumentExtractionFields>().ask(instruction) { hintedPrompt ->
                llm.generate(listOf(LlmPart.Image(bytes), LlmPart.Text(hintedPrompt)))
            }
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

    private fun readImageBytes(uri: String): ByteArray? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { File(it).readBytes() } }.getOrNull()
    }
}
