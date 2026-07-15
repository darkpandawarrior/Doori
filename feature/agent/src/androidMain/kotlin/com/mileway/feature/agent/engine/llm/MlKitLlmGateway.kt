package com.mileway.feature.agent.engine.llm

import android.content.Context
import com.siddharth.kmp.ai.MlKitGenAiOnDeviceLlm
import com.siddharth.kmp.ai.OnDeviceLlm
import kotlinx.coroutines.flow.Flow

// ponytail: delegates the ML Kit GenAI Prompt API call (Gemini Nano) to kmp-toolkit's :ai
// OnDeviceLlm seam (MlKitGenAiOnDeviceLlm) instead of re-deriving Generation.getClient()/
// FeatureStatus/generateContentStream here (A2 consume, mirrors core:ai's #11) — this class now
// only owns the LlmGateway adapter shape, not the model-client plumbing. Compile-verified only,
// NOT device-verified: no Gemini-Nano-class hardware (Pixel 8+/AICore-eligible, locked bootloader)
// is available in this environment — same caveat as MlKitGenAiOnDeviceLlm.
class MlKitLlmGateway(
    context: Context,
    private val llm: OnDeviceLlm = MlKitGenAiOnDeviceLlm(context),
) : LlmGateway {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override fun stream(prompt: String): Flow<String> = llm.generateStream(prompt)
}
