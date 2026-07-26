package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.HardwareEventDao

/**
 * Journey telemetry: drains unsynced [com.mileway.core.data.model.db.HardwareEvent] rows for a
 * token — mirrors [LocationDataSyncer]'s outbox-drain shape ([HardwareEventBatchOutbox] instead of
 * [com.mileway.core.data.outbox.LocationBatchOutbox]), applied to the lifecycle event log
 * ([SmartEventLogger] already writes TRACKING_STARTED/STOPPED/PAUSED/RESUMED/etc. with
 * `uploaded = false`; [HardwareEventDao.getUnsyncedEventsByToken] and
 * [HardwareEventDao.markEventsAsSynced] were sitting unused until this class).
 *
 * Bound in both platform `trackingModule`s under the [HARDWARE_EVENT_OUTBOX] qualifier, with [send]
 * wired to [realHardwareEventSend] — the same shape as [LocationDataSyncer]/[realLocationSend]. The
 * qualifier is required: `SubmitOutbox<T>` resolves by its erased type here, so an unqualified
 * binding shadows `SubmitOutbox<TripDraft>` and hands `MilesSubmitSyncer` the wrong outbox.
 * `KoinGraphTest` catches exactly that — re-run it after touching this wiring.
 *
 * With the default `:stub` binding the POST is a no-op that never throws, so nothing leaves the
 * device until `NetworkBackendFlags.useRealBackend` is flipped — but the graph is now complete.
 *
 * ponytail: unlike [LocationDataSyncer] there's no MAX_BATCHES_PER_DRAIN paging loop — journey
 * lifecycle events are a handful per trip (not a GPS-fix stream), so "every unsynced event for this
 * token in one batch" is enough. Add paging if that assumption ever breaks.
 */
class HardwareEventSyncer(
    private val hardwareEventDao: HardwareEventDao,
    private val outbox: HardwareEventBatchOutbox,
    private val send: suspend (HardwareEventBatch) -> SendOutcome = { SendOutcome.SUCCESS },
) {
    // ponytail: plain Boolean, not a Mutex — same single-dispatcher assumption as
    // LocationDataSyncer.isDraining (every current/intended caller is AppSyncTrigger's
    // main-dispatcher-bound scope); upgrade if a background-thread caller is ever added.
    private var isDraining = false

    /**
     * Sends every still-unsynced [com.mileway.core.data.model.db.HardwareEvent] for [token] as one
     * batch. No-op if [token] is blank, nothing is unsynced, or a drain is already in flight.
     */
    suspend fun drain(token: String) {
        if (token.isEmpty()) return
        if (isDraining) return
        isDraining = true
        try {
            val rows = hardwareEventDao.getUnsyncedEventsByToken(token)
            if (rows.isEmpty()) return
            val ids = rows.map { it.id }
            val batch = HardwareEventBatch(token = token, eventIds = ids)
            val uniqueKey = "$token:${ids.first()}:${ids.last()}"
            outbox.enqueue(formKey = FORM_KEY, uniqueKey = uniqueKey, payload = batch)

            when (send(batch)) {
                SendOutcome.SUCCESS -> {
                    hardwareEventDao.markEventsAsSynced(ids)
                    outbox.markSubmitted(FORM_KEY, uniqueKey)
                }
                SendOutcome.PERMANENT_FAILURE -> {
                    // Reference-app policy (same as LocationDataSyncer): a 409/5xx-equivalent will
                    // never succeed on retry — mark synced anyway so these rows are dropped for
                    // good instead of being re-fetched (and re-enqueued) by every future drain.
                    hardwareEventDao.markEventsAsSynced(ids)
                    outbox.markFailed(FORM_KEY, uniqueKey, error = "permanent")
                }
                SendOutcome.RETRYABLE_FAILURE -> {
                    // Leave unsynced for the next drain call to retry.
                    outbox.markFailed(FORM_KEY, uniqueKey, error = "retryable")
                }
            }
        } finally {
            isDraining = false
        }
    }

    companion object {
        const val FORM_KEY = "hardware_event_batch"
    }
}
