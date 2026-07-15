package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.LocationPayloadV2
import com.mileway.core.data.outbox.LocationBatch
import com.mileway.core.network.api.MilewayNetworkApi
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

// PLAN_V33 A4 reference retry policy: these status codes will never succeed on retry (duplicate
// submit / payload too large / precondition failed / gone) — everything else (5xx, IO errors,
// timeouts, and any other 4xx) is treated as retryable so a transient failure never silently drops
// data; the next drain() call retries the same batch from the same offset.
// internal (not private): PLAN_V33 A5's realMilesSubmitSend shares this exact policy.
internal val PERMANENT_HTTP_STATUSES =
    setOf(HttpStatusCode.Conflict, HttpStatusCode.PayloadTooLarge, HttpStatusCode.PreconditionFailed, HttpStatusCode.NotFound)

/**
 * PLAN_V33 A4: the real `send` for [LocationDataSyncer], replacing the always-SUCCESS stub
 * default. Fetches the batch's rows back from Room, maps each to [LocationPayloadV2] with a
 * deterministic per-record `opId` (the reference app relied on implicit server dedup we don't
 * have; the server enforces `op_id` as a UNIQUE + insert-ignore instead), and posts them through
 * [MilewayNetworkApi.postLocationV2Batch].
 *
 * With the default `:stub` binding ([com.mileway.stub.FakeTrackingNetworkApi]) `postLocationV2Batch`
 * is a no-op that never throws, so this still resolves to [SendOutcome.SUCCESS] — sync behavior is
 * unchanged until `NetworkBackendFlags.useRealBackend` is flipped onto the real Ktor API.
 */
fun realLocationSend(
    api: MilewayNetworkApi,
    locationDao: LocationDao,
): suspend (LocationBatch) -> SendOutcome =
    { batch ->
        val rows = locationDao.getLocationsByIds(batch.pointIds)
        if (rows.isEmpty()) {
            // Nothing left to send (e.g. purged by a maintenance worker between enqueue and send) —
            // there's nothing to fail, so let the drain loop mark this batch done and move on.
            SendOutcome.SUCCESS
        } else {
            try {
                api.postLocationV2Batch(BulkLocationRequestV2(data = rows.map { it.toLocationPayloadV2(batch.token) }))
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

private fun LocationData.toLocationPayloadV2(token: String): LocationPayloadV2 =
    LocationPayloadV2(
        lat = lat,
        lng = lng,
        token = token,
        date = date,
        speed = speed,
        activity = activity,
        isMock = isMock,
        isAbnormal = isAbnormal,
        displacement = displacement,
        accuracy = accuracy,
        provider = provider,
        // Deterministic per-record idempotency key — same shape as LocationBatch's outbox uniqueKey.
        opId = "$token:$id",
    )
