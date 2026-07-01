package com.mileway.feature.agent.engine

import kotlinx.coroutines.flow.Flow

sealed interface AssistantChunk {
    data class Thinking(val phrase: String) : AssistantChunk
    data class Token(val text: String) : AssistantChunk
    data class Done(val fullText: String, val titleSuggestion: String?) : AssistantChunk
}

interface AssistantEngine {
    fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk>
}
