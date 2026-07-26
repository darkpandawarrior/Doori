package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.data.model.network.EventPayloadV2
import com.mileway.core.network.api.MilewayNetworkApi
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException

/**
 * Journey telemetry: the real `send` for [HardwareEventSyncer], mirroring [realLocationSend]'s
 * exact shape for the `hardware_events` table instead of `locations`. Journey start/stop/pause/
 * resume (and anything else SmartEventLogger records) already lands locally with `uploaded = false`
 * — this maps the still-unsynced rows to [EventPayloadV2] with the same deterministic per-record
 * `opId` convention as [realLocationSend] (`"$token:$id"`) so a replayed POST dedupes against the
 * server's `EventsTable.opId` UNIQUE index instead of double-counting, and posts them through
 * [MilewayNetworkApi.postBulkEventsV2]. Reuses [PERMANENT_HTTP_STATUSES] — same retry policy as
 * location sync, defined once in [realLocationSend]'s file.
 *
 * With the default `:stub` binding (`FakeTrackingNetworkApi`) `postBulkEventsV2` is a no-op that
 * never throws, so this still resolves to [SendOutcome.SUCCESS] — unchanged until
 * `NetworkBackendFlags.useRealBackend` is flipped onto the real Ktor API.
 */
fun realHardwareEventSend(
    api: MilewayNetworkApi,
    hardwareEventDao: HardwareEventDao,
): suspend (HardwareEventBatch) -> SendOutcome =
    { batch ->
        val ids = batch.eventIds.toSet()
        val rows = hardwareEventDao.getUnsyncedEventsByToken(batch.token).filter { it.id in ids }
        if (rows.isEmpty()) {
            // Nothing left to send (e.g. already synced by a previous attempt between enqueue and
            // send) — there's nothing to fail, so let the drain loop mark this batch done and move on.
            SendOutcome.SUCCESS
        } else {
            try {
                api.postBulkEventsV2(BulkEventRequestV2(data = rows.map { it.toEventPayloadV2() }))
                SendOutcome.SUCCESS
            } catch (e: CancellationException) {
                throw e
            } catch (e: ClientRequestException) {
                if (e.response.status in PERMANENT_HTTP_STATUSES) SendOutcome.PERMANENT_FAILURE else SendOutcome.RETRYABLE_FAILURE
            } catch (e: Exception) {
                // ServerResponseException (5xx), connect/read timeouts, host unreachable, etc.
                SendOutcome.RETRYABLE_FAILURE
            }
        }
    }

private fun HardwareEvent.toEventPayloadV2(): EventPayloadV2 =
    EventPayloadV2(
        token = token,
        event = event,
        time = time,
        eventType = eventType.name,
        audience = audience.name,
        lat = lat,
        lng = lng,
        metadata = metadata,
        // Deterministic per-record idempotency key — same shape as LocationData's opId (see
        // realLocationSend's toLocationPayloadV2).
        opId = "$token:$id",
    )
