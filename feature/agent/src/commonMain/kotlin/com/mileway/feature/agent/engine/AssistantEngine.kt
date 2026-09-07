package com.mileway.feature.agent.engine

import com.siddharth.kmp.result.AiFailure
import kotlinx.coroutines.flow.Flow

sealed interface AssistantChunk {
    data class Thinking(val phrase: String) : AssistantChunk

    data class Token(val text: String) : AssistantChunk

    data class Done(val fullText: String, val titleSuggestion: String?) : AssistantChunk

    /** The stream ended in failure — before, or after, some [Token]s already emitted. Mirrors
     * kmp-toolkit's [com.siddharth.kmp.llmchat.AiChunk.Failed] shape so a consumer handling both a
     * cloud and an on-device engine reads the same [AiFailure] reason either way. */
    data class Error(val reason: AiFailure) : AssistantChunk
}

interface AssistantEngine {
    fun respond(
        conversationId: String,
        userMessage: String,
        historySize: Int,
    ): Flow<AssistantChunk>
}
