package com.mileway.feature.agent.engine.llm

import com.siddharth.kmp.ai.OnDeviceLlm
import com.siddharth.kmp.ai.UnavailableOnDeviceLlm
import kotlinx.coroutines.flow.Flow

/** Adapts the flavor's on-device backend to the assistant's streaming gateway. */
class OnDeviceLlmGateway(
    private val llm: OnDeviceLlm = UnavailableOnDeviceLlm,
) : LlmGateway {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override fun stream(prompt: String): Flow<String> = llm.generateStream(prompt)
}
