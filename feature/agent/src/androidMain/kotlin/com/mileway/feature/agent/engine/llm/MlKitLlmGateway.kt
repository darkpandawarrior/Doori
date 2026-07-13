package com.mileway.feature.agent.engine.llm

import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

// ponytail: EXPERIMENTAL — the same Gemini Nano model core:ai's MlKitGenAiAnalyzer uses for
// document extraction, here driving free-text assistant replies instead. Compile-verified only,
// NOT device-verified: no Gemini Nano-class hardware (Pixel 8+/AICore-eligible, locked bootloader)
// is available in this environment — same caveat as MlKitGenAiAnalyzer.
//
// [isAvailable] is the same cheap, synchronous SDK-floor check (API's own minSdk 26) — not the
// authoritative runtime gate, since checkStatus() is suspend. The real AVAILABLE-vs-DOWNLOADABLE
// gate lives in [stream]: DOWNLOADABLE/DOWNLOADING/UNAVAILABLE all decline (empty flow) rather than
// trigger a multi-hundred-MB on-demand download mid-conversation.
//
// generateContentStream's per-emission Candidate.text is treated as the incremental delta (typical
// of streaming generation APIs, unverified on-device here) — each emission is forwarded straight
// through as one AssistantChunk.Token, matching LlmAssistantEngine's per-token emit loop.
class MlKitLlmGateway : LlmGateway {
    private val model: GenerativeModel by lazy { Generation.getClient() }

    override fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    override fun stream(prompt: String): Flow<String> =
        flow {
            if (!isAvailable() || model.checkStatus() != FeatureStatus.AVAILABLE) return@flow
            val request = generateContentRequest(TextPart(prompt)) {}
            emitAll(model.generateContentStream(request).map { it.candidates.firstOrNull()?.text.orEmpty() })
        }.catch {
            // On-device model hiccup — degrade to an empty stream; LlmAssistantEngine still emits
            // a Done chunk with whatever text streamed before the failure.
        }
}
