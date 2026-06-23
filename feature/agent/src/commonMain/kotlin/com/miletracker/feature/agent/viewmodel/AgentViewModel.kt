package com.miletracker.feature.agent.viewmodel

import androidx.lifecycle.viewModelScope
import com.miletracker.core.platform.ShareSheet
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.agent.analytics.AgentAnalyticsStore
import com.miletracker.feature.agent.engine.AssistantChunk
import com.miletracker.feature.agent.engine.AssistantEngine
import com.miletracker.feature.agent.model.AgentConversation
import com.miletracker.feature.agent.model.AgentMessage
import com.miletracker.feature.agent.repository.AgentRepository
import com.miletracker.feature.agent.voice.SpeechEvent
import com.miletracker.feature.agent.voice.SpeechToText
import com.miletracker.feature.agent.voice.TextToSpeech
import com.miletracker.feature.agent.voice.stripMarkdownForTts
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val TIMESTAMP_SEED_MS = 1_700_000_000_000L

class AgentViewModel(
    private val repository: AgentRepository,
    private val engine: AssistantEngine,
    private val stt: SpeechToText,
    private val tts: TextToSpeech,
    private val shareSheet: ShareSheet,
    private val analytics: AgentAnalyticsStore,
) : BaseViewModel<AgentUiState, AgentEffect, AgentAction>(
        AgentUiState(
            popularTab = repository.popularQuestions,
            unansweredTab = repository.unansweredQuestions,
        ),
    ) {
    val uiState: StateFlow<AgentUiState> = state

    private var messageCounter = 0L
    private var messagesJob: Job? = null
    private var streamingJob: Job? = null
    private var sttJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val resumeId = repository.getResumableThread(nowMs)
            if (resumeId != null) {
                setState { copy(activeThreadId = resumeId) }
                messagesJob = viewModelScope.launch {
                    repository.messagesFor(resumeId).collect { messages ->
                        setState { copy(messages = messages) }
                    }
                }
            }
        }
        viewModelScope.launch { loadAnalytics() }
        viewModelScope.launch {
            repository.conversationsFlow.collect { list ->
                setState { copy(history = list) }
            }
        }
    }

    override fun onAction(action: AgentAction) {
        when (action) {
            is AgentAction.SendMessage -> sendMessage(action.text)
            is AgentAction.LoadConversation -> loadConversation(action.conversation)
            is AgentAction.DismissError -> setState { copy(error = null) }
            is AgentAction.ResumeThread,
            is AgentAction.StartNewConversation,
            -> Unit
            is AgentAction.SubmitFeedback -> submitFeedback(action.messageId, action.rating, action.comment)
            is AgentAction.StartVoice -> startVoice()
            is AgentAction.StopVoice -> stopVoice()
            is AgentAction.SpeakMessage -> speakMessage(action.messageId)
            is AgentAction.ToggleVoiceConversation -> toggleVoiceConversation()
            is AgentAction.ExportConversation -> exportConversation(action.threadId)
            is AgentAction.SubmitUnanswered -> Unit
            is AgentAction.LoadAnalytics -> loadAnalytics()
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        streamingJob?.cancel()

        val nowMs = TIMESTAMP_SEED_MS + messageCounter * 5000L
        messageCounter++
        val userMsgId = "msg_u_$messageCounter"
        val userMsg = AgentMessage(text = text.trim(), isUser = true, timestampMs = nowMs)

        val historySize = state.value.messages.size
        setState { copy(messages = messages + userMsg, isStreaming = true, streamedText = "", thinkingPhrase = DEFAULT_THINKING) }
        emitEffect(AgentEffect.ScrollToBottom)

        val existingThreadId = state.value.activeThreadId
        val isNewThread = existingThreadId == null
        val threadId = existingThreadId ?: "thread_$nowMs"

        streamingJob = viewModelScope.launch {
            analytics.recordQuestion(text.trim())
            if (isNewThread) {
                repository.createThread(threadId, text.trim().take(50), nowMs)
                setState { copy(activeThreadId = threadId) }
            }
            repository.setActiveThread(threadId, Clock.System.now().toEpochMilliseconds())
            repository.appendMessage(threadId, userMsgId, text.trim(), isUser = true, nowMs)

            var streamedText = ""
            engine.respond(threadId, text.trim(), historySize).collect { chunk ->
                when (chunk) {
                    is AssistantChunk.Thinking ->
                        setState { copy(thinkingPhrase = chunk.phrase) }
                    is AssistantChunk.Token -> {
                        streamedText += chunk.text
                        setState { copy(streamedText = streamedText) }
                    }
                    is AssistantChunk.Done -> {
                        val replyMs = Clock.System.now().toEpochMilliseconds()
                        messageCounter++
                        val replyMsgId = "msg_a_$messageCounter"
                        val assistantMsg = AgentMessage(text = chunk.fullText, isUser = false, timestampMs = replyMs)
                        repository.appendMessage(threadId, replyMsgId, chunk.fullText, isUser = false, replyMs)
                        setState { copy(messages = messages + assistantMsg, isStreaming = false, streamedText = "") }
                        emitEffect(AgentEffect.ScrollToBottom)
                    }
                }
            }
        }
    }

    private fun loadConversation(conversation: AgentConversation) {
        messagesJob?.cancel()
        setState { copy(messages = emptyList(), isStreaming = false, streamedText = "", activeThreadId = conversation.id) }
        messagesJob = viewModelScope.launch {
            repository.setActiveThread(conversation.id, Clock.System.now().toEpochMilliseconds())
            repository.messagesFor(conversation.id).collect { messages ->
                setState { copy(messages = messages) }
            }
        }
    }

    private fun startVoice() {
        sttJob?.cancel()
        setState { copy(isListening = true, voiceTranscript = "", voiceRms = 0f) }
        sttJob = viewModelScope.launch {
            stt.listen().collect { event ->
                when (event) {
                    is SpeechEvent.Partial ->
                        setState { copy(voiceTranscript = event.text) }
                    is SpeechEvent.Final -> {
                        setState { copy(isListening = false, voiceTranscript = "") }
                        val text = event.text
                        if (text.isNotBlank()) {
                            if (state.value.isVoiceConversationMode) {
                                sendMessage(text)
                            } else {
                                emitEffect(AgentEffect.FillInput(text))
                            }
                        }
                    }
                    is SpeechEvent.RmsChanged ->
                        setState { copy(voiceRms = event.rms) }
                    is SpeechEvent.Error ->
                        setState { copy(isListening = false, voiceTranscript = "") }
                }
            }
        }
    }

    private fun stopVoice() {
        stt.stop()
        sttJob?.cancel()
        setState { copy(isListening = false, voiceTranscript = "", voiceRms = 0f) }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val hits = analytics.getHitCounts()
            if (hits.isEmpty()) return@launch
            val sorted = state.value.popularTab.sortedByDescending { q ->
                hits.entries.maxOfOrNull { (k, v) -> if (q.question.lowercase().contains(k.lowercase())) v else 0 } ?: 0
            }
            setState { copy(popularTab = sorted) }
        }
    }

    private fun exportConversation(threadId: String) {
        val messages = state.value.messages
        if (messages.isEmpty()) return
        val transcript = buildString {
            val title = state.value.history.firstOrNull { it.id == threadId }?.title ?: "Conversation"
            appendLine("# $title")
            appendLine()
            messages.forEach { msg ->
                val speaker = if (msg.isUser) "You" else "Mileway"
                appendLine("**$speaker:** ${msg.text}")
                appendLine()
            }
        }
        shareSheet.share(transcript, subject = "Mileway conversation transcript")
    }

    private fun submitFeedback(messageId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            repository.persistFeedback(messageId, rating, comment)
            setState { copy(feedback = feedback + (messageId to rating)) }
        }
    }

    private fun speakMessage(messageId: String) {
        val message = state.value.messages.firstOrNull { it.id == messageId } ?: return
        viewModelScope.launch {
            setState { copy(isSpeaking = true) }
            tts.speak(message.text.stripMarkdownForTts())
            setState { copy(isSpeaking = false) }
        }
    }

    private fun toggleVoiceConversation() {
        val next = !state.value.isVoiceConversationMode
        setState { copy(isVoiceConversationMode = next) }
        if (next) startVoice() else { stopVoice(); tts.stop() }
    }
}
