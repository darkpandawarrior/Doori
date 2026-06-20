package com.miletracker.core.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One permission in a [PermissionOrchestrator] flow and its latest result (null = not yet decided). */
data class PermissionStep(
    val permission: AppPermission,
    val result: PermissionResult? = null,
)

/** Snapshot of a permission flow (UX.3). */
data class PermissionFlowState(
    val steps: List<PermissionStep>,
    val currentIndex: Int,
) {
    val current: PermissionStep? get() = steps.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= steps.size
    val allGranted: Boolean get() = steps.all { it.result == PermissionResult.Granted }
    val denied: List<AppPermission> get() = steps.filter { it.result != PermissionResult.Granted && it.result != null }.map { it.permission }
}

/**
 * Sequential permission-request state machine (UX.3) over a [PermissionsProvider]. Walks a fixed [sequence]
 * one permission at a time: each step is skipped if already granted, otherwise [requestCurrent] asks for it,
 * records the [PermissionResult], and advances. Pure orchestration over the platform provider — no Android /
 * iOS types — so it is fully covered by JVM unit tests with a fake provider. The UI observes [state] and
 * calls [requestCurrent] from a button; [runAll] auto-walks the whole sequence.
 */
class PermissionOrchestrator(
    private val provider: PermissionsProvider,
    private val sequence: List<AppPermission>,
) {
    private val _state = MutableStateFlow(PermissionFlowState(sequence.map { PermissionStep(it) }, 0))
    val state: StateFlow<PermissionFlowState> = _state.asStateFlow()

    /** Advance past any leading permissions that are already granted, without prompting. */
    suspend fun skipAlreadyGranted() {
        while (true) {
            val step = _state.value.current ?: return
            if (provider.isGranted(step.permission)) {
                record(PermissionResult.Granted)
            } else {
                return
            }
        }
    }

    /** Prompt for the current permission, record the result, and advance. No-op once complete. */
    suspend fun requestCurrent(): PermissionResult? {
        val step = _state.value.current ?: return null
        val result = provider.request(step.permission)
        record(result)
        return result
    }

    /** Walk the entire sequence: skip granted ones, request the rest, stop at the end. */
    suspend fun runAll() {
        while (_state.value.current != null) {
            val step = _state.value.current ?: break
            if (provider.isGranted(step.permission)) {
                record(PermissionResult.Granted)
            } else {
                record(provider.request(step.permission))
            }
        }
    }

    private fun record(result: PermissionResult) {
        _state.value =
            _state.value.let { s ->
                val updated = s.steps.toMutableList().also { it[s.currentIndex] = it[s.currentIndex].copy(result = result) }
                PermissionFlowState(updated, s.currentIndex + 1)
            }
    }
}
