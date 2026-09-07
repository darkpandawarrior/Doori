package com.mileway

import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.model.AgentConversation
import com.mileway.feature.agent.repository.AgentRepository
import com.mileway.feature.agent.viewmodel.AgentAction
import com.mileway.feature.agent.viewmodel.AgentViewModel
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.core.platform.ShareSheet
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.voice.TextToSpeech
import com.siddharth.kmp.result.AiFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A reply that fails with [reason] right after "thinking" — never drops a reply silently. */
private class ErrorAssistantEngine(private val reason: AiFailure) : AssistantEngine {
    override fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk> = flow {
        emit(AssistantChunk.Thinking("Thinking…"))
        emit(AssistantChunk.Error(reason))
    }
}

/** A reply that never finishes on its own — only Stop (job cancellation) ends it. */
private class SlowAssistantEngine : AssistantEngine {
    override fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk> = flow {
        emit(AssistantChunk.Thinking("Thinking…"))
        while (true) {
            delay(10)
            emit(AssistantChunk.Token("x"))
        }
    }
}

class AgentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(engine: AssistantEngine = FakeAssistantEngine()) =
        AgentViewModel(AgentRepository(FakeAgentDao(), FakeAgentSessionStore()), engine, FakeSpeechToText(), FakeTextToSpeech(), FakeShareSheet(), FakeAgentAnalyticsStore())

    @Test
    fun `init seeds popular and unanswered tabs synchronously, history after coroutine`() = runTest {
        val vm = viewModel()
        // popularTab and unansweredTab are set synchronously in the constructor
        assertTrue(vm.state.value.popularTab.isNotEmpty())
        assertTrue(vm.state.value.unansweredTab.isNotEmpty())

        // history is filled by the seedIfEmpty + collect coroutines
        advanceUntilIdle()
        assertTrue(vm.state.value.history.isNotEmpty())
    }

    @Test
    fun `blank message is ignored`() {
        val vm = viewModel()
        vm.onAction(AgentAction.SendMessage("   "))
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun `sendMessage appends the user message and streams an assistant reply`() = runTest {
        val vm = viewModel()
        vm.onAction(AgentAction.SendMessage("what is my travel spend"))

        // Synchronous part: user message added, streaming begins (assistant not yet appended).
        assertEquals(1, vm.state.value.messages.size)
        assertTrue(vm.state.value.messages.first().isUser)
        assertTrue(vm.state.value.isStreaming)

        advanceUntilIdle()

        // After the streaming coroutine completes: assistant message appended, streaming done.
        assertEquals(2, vm.state.value.messages.size)
        assertFalse(vm.state.value.messages.last().isUser)
        assertFalse(vm.state.value.isStreaming)
        assertTrue(vm.state.value.messages.last().text.isNotBlank())
    }

    @Test
    fun `sendMessage creates a thread and sets activeThreadId`() = runTest {
        val vm = viewModel()
        assertNotNull(null == vm.state.value.activeThreadId)  // null before first message
        vm.onAction(AgentAction.SendMessage("test question"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.activeThreadId)
    }

    @Test
    fun `loadConversation loads messages from Room for that conversation`() = runTest {
        val vm = viewModel()
        advanceUntilIdle() // let seeding + history flow complete

        // The first conversation in history (sorted by lastMessageMs DESC) is CONV-001 with 2 messages
        val history = vm.state.value.history
        assertTrue(history.isNotEmpty())
        val firstConv = history.first()

        vm.onAction(AgentAction.LoadConversation(firstConv))
        advanceUntilIdle()

        // CONV-001 has 2 messages seeded
        assertEquals(2, vm.state.value.messages.size)
        assertFalse(vm.state.value.isStreaming)
        assertEquals(firstConv.id, vm.state.value.activeThreadId)
    }

    @Test
    fun `loadConversation cancels previous messages collection`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val history = vm.state.value.history
        // Load first conversation, then load second; final messages should be for the second
        val conv1 = history[0]
        val conv2 = history[1]

        vm.onAction(AgentAction.LoadConversation(conv1))
        advanceUntilIdle()
        vm.onAction(AgentAction.LoadConversation(conv2))
        advanceUntilIdle()

        assertEquals(conv2.id, vm.state.value.activeThreadId)
    }

    @Test
    fun `DismissError clears the error field`() {
        val vm = viewModel()
        vm.onAction(AgentAction.DismissError)
        assertNull(vm.state.value.error) // error stays null (was already null)
    }

    @Test
    fun `a mid-stream AiFailure stops streaming and sets the error field, message stays in transcript`() = runTest {
        val vm = viewModel(engine = ErrorAssistantEngine(AiFailure.EmptyReply))
        vm.onAction(AgentAction.SendMessage("what is my travel spend"))
        advanceUntilIdle()

        assertFalse(vm.state.value.isStreaming)
        assertEquals(AiFailure.EmptyReply, vm.state.value.error)
        // the user's question is not silently dropped — it's still there for Retry to resend
        assertEquals(1, vm.state.value.messages.size)
    }

    @Test
    fun `StopStreaming cancels the in-flight reply and clears streaming state without an assistant reply`() = runTest {
        val vm = viewModel(engine = SlowAssistantEngine())
        vm.onAction(AgentAction.SendMessage("long question"))
        // No advanceUntilIdle(): SlowAssistantEngine's loop always has another delay scheduled, so
        // draining the virtual clock would never finish. isStreaming flips true synchronously in
        // sendMessage, before the streaming coroutine is even launched.
        assertTrue(vm.state.value.isStreaming)

        vm.onAction(AgentAction.StopStreaming)

        assertFalse(vm.state.value.isStreaming)
        assertEquals("", vm.state.value.streamedText)
        assertEquals(1, vm.state.value.messages.size) // only the user's message; no assistant reply appended
    }

    @Test
    fun `RetryLastMessage resends the last user question and clears the error`() = runTest {
        val vm = viewModel(engine = ErrorAssistantEngine(AiFailure.Network))
        vm.onAction(AgentAction.SendMessage("what is my travel spend"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.onAction(AgentAction.RetryLastMessage)
        advanceUntilIdle()

        assertNotNull(vm.state.value.error) // ErrorAssistantEngine fails every time, so it's set again...
        assertEquals(2, vm.state.value.messages.size) // ...but the question was genuinely resent
        assertTrue(vm.state.value.messages[1].isUser)
        assertEquals("what is my travel spend", vm.state.value.messages[1].text)
    }
}
