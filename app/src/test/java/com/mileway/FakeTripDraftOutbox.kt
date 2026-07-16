package com.mileway

import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.outbox.TripDraftOutbox
import com.siddharth.kmp.offlineoutbox.DraftEntry
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [TripDraftOutbox] — mirrors feature/tracking's commonTest FakeTripDraftOutbox
 * (not visible cross-module from app/test). A relaxed mockk's `drafts()` returns a Flow that never
 * emits, which crashes SyncStatusViewModel's eager `MilesSubmitSyncer.drain()` -> `.first()` call
 * (memory: screenshot Koin needs deterministic fakes, same trap as FakeVehicleDetailsDao etc.).
 * MutableStateFlow always has a current value, so that `.first()` call resolves instead of throwing.
 */
class FakeTripDraftOutbox : TripDraftOutbox {
    private val entries = MutableStateFlow<Map<String, DraftEntry<TripDraft>>>(emptyMap())

    override fun drafts(formKey: String): Flow<List<DraftEntry<TripDraft>>> =
        entries.map { it.values.filter { e -> e.formKey == formKey } }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: TripDraft,
    ) {
        entries.value = entries.value + (uniqueKey to DraftEntry(formKey, uniqueKey, payload, DraftStatus.PENDING, null, 0L, 0L))
    }

    override suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    ) {
        entries.value = entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.SUBMITTED))
    }

    override suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    ) {
        entries.value =
            entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.FAILED, errorMessage = error))
    }

    override suspend fun clear(formKey: String) {
        entries.value = entries.value.filterValues { it.formKey != formKey }
    }
}
