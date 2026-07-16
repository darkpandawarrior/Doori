package com.mileway.feature.whatsnew.data

import com.siddharth.kmp.offlineoutbox.OpEntry
import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [OpOutbox] test double. Captures every [enqueue] call — the only method
 * [WhatsNewEngagementRecorder] (and its callers) exercise; [pending]/[deadLetters]/[replay]/
 * [requeue] are unused by this feature's tests, so they're stubbed rather than modelled.
 */
class FakeOpOutbox : OpOutbox {
    data class Enqueued(val type: String, val payload: String)

    val enqueued = mutableListOf<Enqueued>()

    override suspend fun enqueue(
        type: String,
        payload: String,
    ): String {
        enqueued += Enqueued(type, payload)
        return "fake-${enqueued.size}"
    }

    override fun pending(): Flow<List<OpEntry>> = MutableStateFlow(emptyList())

    override suspend fun deadLetters(): List<OpEntry> = emptyList()

    override suspend fun replay(
        maxAttempts: Int,
        isPermanent: (Throwable) -> Boolean,
        send: suspend (OpEntry) -> Unit,
    ) = Unit

    override suspend fun requeue(id: String) = Unit
}
