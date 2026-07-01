package com.mileway

import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.AssistantEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAssistantEngine : AssistantEngine {
    override fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk> = flow {
        emit(AssistantChunk.Thinking("Thinking…"))
        val text = "This is a test reply."
        text.split(" ").forEach { word -> emit(AssistantChunk.Token("$word ")) }
        val title = if (historySize == 0) "Test Conversation" else null
        emit(AssistantChunk.Done(text, title))
    }
}
