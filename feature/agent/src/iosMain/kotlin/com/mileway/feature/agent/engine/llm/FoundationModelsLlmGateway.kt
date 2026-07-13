package com.mileway.feature.agent.engine.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// EXPERIMENTAL — Apple Foundation Models, same "no platform.FoundationModels.* Kotlin/Native
// cinterop binding" gap as core:ai's FoundationModelsAnalyzer — the real actual is a Swift class
// conforming to [TextGenerator], injected into [FoundationModelsTextGeneratorBridge] at app startup.
// See `iosApp/iosApp/ai/FoundationModelsTextGenerator.swift`.
object FoundationModelsTextGeneratorBridge {
    val seam = InjectableTextGenerator()
}

// [TextGenerator.generate] returns the whole response as one String (LanguageModelSession.respond
// isn't threaded through this seam incrementally) — this degrades gracefully to a single
// AssistantChunk.Token instead of true per-token streaming. Revisit if/when a streaming variant of
// the Swift bridge (LanguageModelSession.streamResponse) is worth the added completion-handler
// plumbing.
class FoundationModelsLlmGateway : LlmGateway {
    override fun isAvailable(): Boolean = FoundationModelsTextGeneratorBridge.seam.isAvailable()

    override fun stream(prompt: String): Flow<String> =
        flow {
            val text = FoundationModelsTextGeneratorBridge.seam.generate(prompt)
            if (!text.isNullOrEmpty()) emit(text)
        }
}
