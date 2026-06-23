package com.miletracker.feature.agent.engine.llm

import kotlinx.coroutines.flow.Flow

interface LlmGateway {
    fun stream(prompt: String): Flow<String>
}
