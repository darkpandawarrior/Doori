package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.outbox.LocationBatch
import com.mileway.core.data.outbox.LocationBatchOutbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wave-4 §2.3: drains the location outbox into the (stubbed) sync target. Idle / syncing / result. */
sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    data class Synced(val lastSyncedAtMs: Long, val backlogCount: Int) : SyncStatus
}

/** A batch send outcome — mirrors the reference app's outbox retry decision surface. */
enum class SendOutcome { SUCCESS, RETRYABLE_FAILURE, PERMANENT_FAILURE }

/**
 * Drains unsynced [com.mileway.core.data.model.db.LocationData] rows in bounded batches, applying
 * the reference app's batching + retry policy on top of the generic [LocationBatchOutbox]:
 * - at most [MAX_POINTS_PER_CALL] points per send call
 * - at most [MAX_BATCHES_PER_DRAIN] batches per [drain] invocation
 * - at least [MIN_SYNC_GAP_MS] between drains (a no-op call inside the gap is a no-op, not an error)
 * - a send outcome of [SendOutcome.PERMANENT_FAILURE] (409/5xx-equivalent) drops the batch for good
 *   instead of retrying it forever
 *
 * [send] is the sync target — a `:stub` function today (no real HTTP; see class-level module doc).
 */
class LocationDataSyncer(
    private val locationDao: LocationDao,
    private val outbox: LocationBatchOutbox,
    private val now: () -> Long,
    private val send: suspend (LocationBatch) -> SendOutcome = { SendOutcome.SUCCESS },
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var lastDrainAtMs: Long = Long.MIN_VALUE / 2

    /**
     * Drains up to [MAX_BATCHES_PER_DRAIN] batches of up to [MAX_POINTS_PER_CALL] unsynced points
     * for [token]. Returns immediately (no-op) if called again inside [MIN_SYNC_GAP_MS] of the last
     * drain — callers can poll freely without violating the min-gap policy.
     */
    suspend fun drain(token: String) {
        val nowMs = now()
        if (nowMs - lastDrainAtMs < MIN_SYNC_GAP_MS) return

        _syncStatus.value = SyncStatus.Syncing
        var batchesSent = 0
        var offset = 0
        while (batchesSent < MAX_BATCHES_PER_DRAIN) {
            val page = locationDao.getUnsyncedLocationsByTokenPaged(token, limit = MAX_POINTS_PER_CALL, offset = offset)
            if (page.isEmpty()) break

            val batch = LocationBatch(token = token, pointIds = page.map { it.id })
            val uniqueKey = "$token:${batch.pointIds.first()}:${batch.pointIds.last()}"
            outbox.enqueue(formKey = FORM_KEY, uniqueKey = uniqueKey, payload = batch)

            when (send(batch)) {
                SendOutcome.SUCCESS -> {
                    locationDao.markLocationsAsSynced(batch.pointIds)
                    outbox.markSubmitted(FORM_KEY, uniqueKey)
                }
                SendOutcome.PERMANENT_FAILURE -> {
                    // Reference-app policy: 409/5xx-equivalent means the batch will never succeed —
                    // drop it for good. Points stay "unsynced" in storage but skipped by offsetting
                    // past them, so the drain loop still makes progress instead of re-fetching them.
                    outbox.markFailed(FORM_KEY, uniqueKey, error = "permanent")
                    offset += page.size
                }
                SendOutcome.RETRYABLE_FAILURE -> {
                    // Leave in place for the next drain call to retry from the same offset.
                    outbox.markFailed(FORM_KEY, uniqueKey, error = "retryable")
                    break
                }
            }
            batchesSent++
        }

        lastDrainAtMs = nowMs
        val backlog = locationDao.getUnuploadedLocationCount()
        _syncStatus.value = SyncStatus.Synced(lastSyncedAtMs = nowMs, backlogCount = backlog)
    }

    companion object {
        const val MAX_POINTS_PER_CALL = 120
        const val MAX_BATCHES_PER_DRAIN = 50
        const val MIN_SYNC_GAP_MS = 30_000L
        const val FORM_KEY = "location_batch"
    }
}
