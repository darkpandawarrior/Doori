package com.mileway.core.ui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * V32 SN: cross-module signal a top-level tab screen uses to suppress the shell's floating
 * bubble bar while it shows its own pinned contextual bottom bar (e.g. Approvals' bulk-selection
 * action row), avoiding two bottom bars stacking. Same singleton-StateFlow shape as
 * `AssistantFabSessionState`, homed in `core:ui` since it's shell-shared, not feature-owned.
 */
object ShellBottomBarState {
    private val _contextualBarActive = MutableStateFlow(false)
    val contextualBarActive: StateFlow<Boolean> = _contextualBarActive.asStateFlow()

    fun setContextualBarActive(active: Boolean) {
        _contextualBarActive.value = active
    }
}
