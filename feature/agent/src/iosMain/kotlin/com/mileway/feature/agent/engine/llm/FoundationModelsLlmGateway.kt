package com.mileway.feature.agent.engine.llm

import com.siddharth.kmp.ai.FoundationModelsOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import kotlinx.coroutines.flow.Flow

// EXPERIMENTAL — Apple Foundation Models. Delegates to kmp-toolkit's :ai OnDeviceLlm seam
// (FoundationModelsOnDeviceLlm), same engine core:ai's FoundationModelsAnalyzer uses for document
// extraction — both now share ONE Swift bridge (com.siddharth.kmp.ai.FoundationModelsBridge,
// registered once in AppDelegate.swift) instead of this app's own separate InjectableTextGenerator/
// FoundationModelsTextGeneratorBridge seam. Mirrors MlKitLlmGateway's shape exactly.
//
// FoundationModelsOnDeviceLlm.generateStream is REAL per-token streaming through the injected
// bridge (LanguageModelSession.streamResponse, diffed into suffixes) — an upgrade over the old
// Swift TextGenerator seam, which only ever replayed one whole-response emit.
class FoundationModelsLlmGateway(
    private val llm: OnDeviceLlm = FoundationModelsOnDeviceLlm(),
) : LlmGateway {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override fun stream(prompt: String): Flow<String> = llm.generateStream(prompt)
}
