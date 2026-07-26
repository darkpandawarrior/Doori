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
 * NOT YET DI-REGISTERED — this class and [realHardwareEventSend] are built and unit-tested, but no
 * `trackingModule` binds them, so journey start/stop/pause/resume telemetry still never leaves the
 * device. Wiring it needs care: registering a second `SubmitOutbox` type in `trackingModule` collides
 * with the `SubmitOutbox<TripDraft>` binding at resolution time and breaks `KoinGraphTest` (the app
 * graph then hands `MilesSubmitSyncer` the wrong outbox). Bind it with an explicit Koin qualifier, and
 * re-run `KoinGraphTest` — it is the thing that catches this class of mistake.
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
