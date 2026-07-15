package com.mileway.core.ai

import android.content.Context
import android.net.Uri
import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentImageRef
import com.mileway.core.ai.model.ExtractedValue
import com.siddharth.kmp.ai.LlmPart
import com.siddharth.kmp.ai.MlKitGenAiOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import java.io.File

// ponytail: EXPERIMENTAL — delegates the ML Kit GenAI Prompt API call (Gemini Nano) to
// kmp-toolkit's :ai OnDeviceLlm seam (MlKitGenAiOnDeviceLlm) instead of re-deriving
// Generation.getClient()/FeatureStatus/GenerateContentRequest here (#11 consume) — this module
// now only owns document-scan prompt building + JSON-field parsing, not the model-client
// plumbing. Compile-verified only, NOT device-verified: no Gemini-Nano-class hardware (Pixel
// 8+/AICore-eligible, locked bootloader) is available in this environment.
class MlKitGenAiAnalyzer(
    private val context: Context,
    private val llm: OnDeviceLlm = MlKitGenAiOnDeviceLlm(context),
) : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? {
        if (!isAvailable()) return null
        return runCatching { runExtraction(image, prompt) }.getOrNull()
    }

    private suspend fun runExtraction(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? {
        val bytes = readImageBytes(image) ?: return null
        val parts = listOf(LlmPart.Image(bytes), LlmPart.Text("${prompt.instruction}\n\n${prompt.schemaHint}"))
        val text = llm.generate(parts)
        if (text.isNullOrBlank()) return null
        return AiExtraction(
            docType = parseDocType(text),
            fields = parseFields(text),
            rawText = text,
            confidence = RESPONSE_CONFIDENCE,
        )
    }

    private fun readImageBytes(uri: String): ByteArray? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { File(it).readBytes() } }.getOrNull()
    }

    // ponytail: flat "key": "value" regex scrape of the model's JSON text — no kotlinx.serialization
    // dependency for a schema that isn't fixed yet (no DocPrompt template ships a concrete JSON shape
    // in this codebase today). Upgrade to a real parser once a schemaHint locks in nested/typed output.
    private fun parseFields(json: String): Map<DocField, ExtractedValue> {
        val fields = mutableMapOf<DocField, ExtractedValue>()
        for (match in JSON_STRING_FIELD.findAll(json)) {
            val (key, value) = match.destructured
            if (value.isBlank()) continue
            val field = DocField.entries.find { it.name.equals(key, ignoreCase = true) } ?: continue
            fields[field] = ExtractedValue(value, RESPONSE_CONFIDENCE, AnalyzerSource.ON_DEVICE_AI)
        }
        return fields
    }

    private fun parseDocType(json: String): DocType? {
        val key = DOC_TYPE_FIELD.find(json)?.groupValues?.get(1) ?: return null
        return DocType.entries.find { it.name.equals(key, ignoreCase = true) }
    }

    private companion object {
        // Above AnalysisCombiner.AI_CONFIDENT_THRESHOLD (0.6) so a confident docType/field call
        // actually wins the merge; still leaves room to tune once device output is observed.
        const val RESPONSE_CONFIDENCE = 0.7f
        val JSON_STRING_FIELD = Regex(""""([A-Za-z_]+)"\s*:\s*"([^"]*)"""")
        val DOC_TYPE_FIELD = Regex(""""docType"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
    }
}
