package com.miletracker.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI base. `S` = UiState, `E` = Effect, `A` = user Action.
 *
 * Two effect channels with distinct delivery contracts:
 * - [emitEffect] → a buffered [Channel] consumed **exactly once** by the screen's `LaunchedEffect`.
 *   Use for navigation, toasts, one-shot dialogs — things that must fire once and never replay.
 * - [emitBroadcast] → a [SharedFlow] (no replay, drops oldest on overflow) that **survives config
 *   change** and is delivered to whatever collector is active. Use for fire-and-forget signals where
 *   a missed emission is acceptable but new collectors should still receive subsequent ones.
 */
abstract class BaseViewModel<S : Any, E : Any, A : Any>(initial: S) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    private val _broadcast =
        MutableSharedFlow<E>(
            replay = 0,
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val broadcast: SharedFlow<E> = _broadcast.asSharedFlow()

    protected val currentState: S get() = _state.value

    protected fun setState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    /** Deliver once to the active screen collector (navigation / toast / one-shot dialog). */
    protected fun emitEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }

    /** Deliver to current collectors; survives config change, no replay. */
    protected fun emitBroadcast(effect: E) {
        _broadcast.tryEmit(effect)
    }

    abstract fun onAction(action: A)
}
