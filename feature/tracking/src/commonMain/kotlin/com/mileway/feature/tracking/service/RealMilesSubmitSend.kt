package com.mileway.feature.tracking.service

import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.network.api.MilewayNetworkApi
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException

/**
 * PLAN_V33 A5: the real `send` for [MilesSubmitSyncer] — posts the queued [TripDraft]'s request
 * through [MilewayNetworkApi.submitMiles] and classifies the HTTP outcome with the exact same
 * retry policy as [realLocationSend] ([PERMANENT_HTTP_STATUSES], defined there).
 *
 * With the default `:stub` binding ([com.mileway.stub.FakeTrackingNetworkApi]) `submitMiles` always
 * succeeds, so this still resolves to [SubmitOutcome.Success] — demo-mode submit behavior is
 * unchanged until `NetworkBackendFlags.useRealBackend` is flipped onto the real Ktor API.
 */
fun realMilesSubmitSend(api: MilewayNetworkApi): suspend (TripDraft) -> SubmitOutcome =
    { draft ->
        try {
            SubmitOutcome.Success(api.submitMiles(draft.request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            if (e.response.status in PERMANENT_HTTP_STATUSES) SubmitOutcome.PermanentFailure else SubmitOutcome.RetryableFailure
        } catch (e: Exception) {
            // ServerResponseException (5xx), connect/read timeouts, host unreachable, etc.
            SubmitOutcome.RetryableFailure
        }
    }
