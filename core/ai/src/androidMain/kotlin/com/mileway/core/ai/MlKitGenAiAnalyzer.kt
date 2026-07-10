package com.mileway.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentImageRef
import com.mileway.core.ai.model.ExtractedValue

// ponytail: EXPERIMENTAL — com.google.mlkit:genai-prompt:1.0.0-beta2 (ML Kit GenAI Prompt API,
// Gemini Nano). Compile-verified only, NOT device-verified: no Gemini Nano-class hardware (Pixel
// 8+/AICore-eligible, locked bootloader) is available in this environment. Revisit device
// behavior — response JSON quality, checkStatus() transition timing — when such a device exists.
//
// [isAvailable] is a cheap, synchronous device-tier floor (matches the API's own minSdk 26) — NOT
// the authoritative runtime check, since ML Kit's own `checkStatus()` is suspend and this
// interface method isn't. The real gate lives in [extract]: it only runs inference when the
// feature is already AVAILABLE (model resident). DOWNLOADABLE/DOWNLOADING/UNAVAILABLE all decline
// rather than trigger a multi-hundred-MB on-demand download mid-scan — there's no download-progress
// UX yet, so pre-warming stays out of scope for this task.
class MlKitGenAiAnalyzer(private val context: Context) : DocumentAiAnalyzer {
    private val model: GenerativeModel by lazy { Generation.getClient() }

    override fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

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
        if (model.checkStatus() != FeatureStatus.AVAILABLE) return null
        val bitmap = decodeBitmap(image) ?: return null
        val text =
            try {
                val request = buildRequest(bitmap, prompt)
                model.generateContent(request).candidates.firstOrNull()?.text
            } finally {
                bitmap.recycle()
            }
        if (text.isNullOrBlank()) return null
        return AiExtraction(
            docType = parseDocType(text),
            fields = parseFields(text),
            rawText = text,
            confidence = RESPONSE_CONFIDENCE,
        )
    }

    private fun buildRequest(
        bitmap: Bitmap,
        prompt: DocPrompt,
    ): GenerateContentRequest =
        generateContentRequest(
            ImagePart(bitmap),
            TextPart("${prompt.instruction}\n\n${prompt.schemaHint}"),
        ) {}

    private fun decodeBitmap(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        return runCatching {
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
            ?: runCatching { parsed.path?.let { BitmapFactory.decodeFile(it) } }.getOrNull()
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
