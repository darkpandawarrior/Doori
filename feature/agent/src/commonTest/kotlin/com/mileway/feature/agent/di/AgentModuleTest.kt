package com.mileway.feature.agent.di

import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.engine.llm.LlmAssistantEngine
import com.mileway.feature.agent.engine.llm.LlmGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertIs

class AgentModuleTest {
    private class FakeGateway(private val available: Boolean) : LlmGateway {
        override fun isAvailable(): Boolean = available

        override fun stream(prompt: String): Flow<String> = emptyFlow()
    }

    private object FakeOfflineEngine : AssistantEngine {
        override fun respond(
            conversationId: String,
            userMessage: String,
            historySize: Int,
        ) = emptyFlow<com.mileway.feature.agent.engine.AssistantChunk>()
    }

    @Test
    fun picksLlmEngineWhenGatewayAvailable() {
        val engine = selectAssistantEngine(FakeGateway(available = true)) { FakeOfflineEngine }

        assertIs<LlmAssistantEngine>(engine)
    }

    @Test
    fun picksOfflineEngineWhenGatewayUnavailable() {
        val engine = selectAssistantEngine(FakeGateway(available = false)) { FakeOfflineEngine }

        assertIs<FakeOfflineEngine>(engine)
    }
}
