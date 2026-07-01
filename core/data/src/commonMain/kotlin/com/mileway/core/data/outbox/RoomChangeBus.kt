package com.mileway.core.data.outbox

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Post-write UI-refresh bus. Emit a set of changed table names after any write;
// collectors (e.g. ViewModels) react deterministically without polling.
object RoomChangeBus {
    private val _changes = MutableSharedFlow<Set<String>>(extraBufferCapacity = 32)
    val changes: SharedFlow<Set<String>> = _changes.asSharedFlow()

    fun notify(vararg tables: String) {
        _changes.tryEmit(tables.toSet())
    }
}
