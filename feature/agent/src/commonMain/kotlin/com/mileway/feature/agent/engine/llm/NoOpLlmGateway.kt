package com.mileway.feature.agent.engine.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NoOpLlmGateway : LlmGateway {
    override fun isAvailable(): Boolean = false

    override fun stream(prompt: String): Flow<String> = emptyFlow()
}
