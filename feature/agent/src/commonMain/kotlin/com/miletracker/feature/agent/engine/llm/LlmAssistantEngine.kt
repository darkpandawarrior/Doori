package com.miletracker.feature.agent.engine.llm

import com.miletracker.feature.agent.engine.AssistantChunk
import com.miletracker.feature.agent.engine.AssistantEngine
import com.miletracker.feature.agent.engine.ConversationTitler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LlmAssistantEngine(private val gateway: LlmGateway) : AssistantEngine {

    override fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk> = flow {
        emit(AssistantChunk.Thinking("Thinking…"))
        var fullText = ""
        gateway.stream(userMessage).collect { token ->
            fullText += token
            emit(AssistantChunk.Token(token))
        }
        val title = if (historySize == 0) ConversationTitler.title(userMessage) else null
        emit(AssistantChunk.Done(fullText, title))
    }
}
