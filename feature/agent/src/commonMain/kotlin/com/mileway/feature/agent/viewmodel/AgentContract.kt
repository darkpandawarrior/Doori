package com.mileway.feature.agent.viewmodel

import com.mileway.feature.agent.model.AgentConversation
import com.mileway.feature.agent.model.AgentMessage
import com.mileway.feature.agent.model.PopularQuestion
import com.mileway.feature.agent.model.UnansweredQuestion
import com.siddharth.kmp.result.AiFailure

internal const val DEFAULT_THINKING = "Thinking…"

data class AgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val streamedText: String = "",
    val thinkingPhrase: String = DEFAULT_THINKING,
    val popularTab: List<PopularQuestion> = emptyList(),
    val unansweredTab: List<UnansweredQuestion> = emptyList(),
    val history: List<AgentConversation> = emptyList(),
    val activeThreadId: String? = null,
    val streamingMessageId: String? = null,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isVoiceConversationMode: Boolean = false,
    val voiceTranscript: String = "",
    val voiceRms: Float = 0f,
    val feedback: Map<String, Int> = emptyMap(),
    val error: AiFailure? = null,
    val isServiceReady: Boolean = true,
    val serviceStatusMessage: String? = null,
)

sealed interface AgentAction {
    data class SendMessage(val text: String) : AgentAction

    /** Cancels the in-flight [LlmAssistantEngine][com.mileway.feature.agent.engine.llm.LlmAssistantEngine]
     * (or offline) reply. Whatever streamed so far is discarded, not kept as a partial message. */
    data object StopStreaming : AgentAction

    /** Re-sends the last user question after an [AssistantChunk.Error][com.mileway.feature.agent.engine.AssistantChunk.Error]. */
    data object RetryLastMessage : AgentAction

    data class LoadConversation(val conversation: AgentConversation) : AgentAction

    // P0.3 additions — handled in later phases
    data class ResumeThread(val threadId: String) : AgentAction

    data object StartNewConversation : AgentAction

    data class SubmitFeedback(val messageId: String, val rating: Int, val comment: String? = null) : AgentAction

    data object StartVoice : AgentAction

    data object StopVoice : AgentAction

    data class SpeakMessage(val messageId: String) : AgentAction

    data object ToggleVoiceConversation : AgentAction

    data class ExportConversation(val threadId: String) : AgentAction

    data class SubmitUnanswered(val question: String) : AgentAction

    data object LoadAnalytics : AgentAction

    data object DismissError : AgentAction
}

sealed interface AgentEffect {
    data object ScrollToBottom : AgentEffect

    data class ShareTranscript(val path: String) : AgentEffect

    data class ShowSnackbar(val text: String) : AgentEffect

    data class FillInput(val text: String) : AgentEffect
}
