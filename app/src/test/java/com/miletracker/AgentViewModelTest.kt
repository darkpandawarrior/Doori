package com.miletracker

import com.miletracker.feature.agent.repository.AgentRepository
import com.miletracker.feature.agent.viewmodel.AgentAction
import com.miletracker.feature.agent.viewmodel.AgentViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * H: behavioural coverage for [AgentViewModel], the assistant chat reducer with a simulated streaming
 * reply. The repository is a concrete in-memory mock (no deps).
 */
class AgentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = AgentViewModel(AgentRepository())

    @Test
    fun `init seeds the popular, unanswered and history tabs from the repository`() {
        val state = viewModel().state.value
        assertTrue(state.popularTab.isNotEmpty())
        assertTrue(state.unansweredTab.isNotEmpty())
        assertTrue(state.history.isNotEmpty())
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
    fun `loadConversation replaces the visible messages`() {
        val vm = viewModel()
        val conversation = AgentRepository().conversations.first()
        vm.onAction(AgentAction.LoadConversation(conversation))
        assertEquals(conversation.messages, vm.state.value.messages)
        assertFalse(vm.state.value.isStreaming)
    }
}
