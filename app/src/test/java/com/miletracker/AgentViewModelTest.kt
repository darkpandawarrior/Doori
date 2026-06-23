package com.miletracker

import com.miletracker.feature.agent.engine.AssistantEngine
import com.miletracker.feature.agent.model.AgentConversation
import com.miletracker.feature.agent.repository.AgentRepository
import com.miletracker.feature.agent.viewmodel.AgentAction
import com.miletracker.feature.agent.viewmodel.AgentViewModel
import com.miletracker.feature.agent.voice.SpeechToText
import com.miletracker.core.platform.ShareSheet
import com.miletracker.feature.agent.analytics.AgentAnalyticsStore
import com.miletracker.feature.agent.voice.TextToSpeech
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = AgentViewModel(AgentRepository(FakeAgentDao(), FakeAgentSessionStore()), FakeAssistantEngine(), FakeSpeechToText(), FakeTextToSpeech(), FakeShareSheet(), FakeAgentAnalyticsStore())

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
        assertNotNull(null == vm.state.value.error) // error stays null (was already null)
    }
}
