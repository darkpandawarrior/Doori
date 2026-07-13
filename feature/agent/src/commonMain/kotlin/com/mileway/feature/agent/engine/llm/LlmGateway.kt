package com.mileway.feature.agent.engine.llm

import kotlinx.coroutines.flow.Flow

interface LlmGateway {
    /** Cheap, synchronous capability check — [AgentModule] uses this to pick [LlmAssistantEngine]
     * over the [com.mileway.feature.agent.engine.OfflineAssistantEngine] degrade path. */
    fun isAvailable(): Boolean

    fun stream(prompt: String): Flow<String>
}
