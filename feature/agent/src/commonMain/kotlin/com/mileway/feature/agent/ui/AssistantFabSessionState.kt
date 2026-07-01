package com.mileway.feature.agent.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssistantEntryMode { FAB, TOPBAR }

object AssistantFabSessionState {
    private val _mode = MutableStateFlow(AssistantEntryMode.FAB)
    val mode: StateFlow<AssistantEntryMode> = _mode.asStateFlow()

    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    fun switchToTopbar() { _mode.value = AssistantEntryMode.TOPBAR }
    fun switchToFab() { _mode.value = AssistantEntryMode.FAB }
    fun onChatOpen() { _isChatOpen.value = true }
    fun onChatClose() { _isChatOpen.value = false }
}
