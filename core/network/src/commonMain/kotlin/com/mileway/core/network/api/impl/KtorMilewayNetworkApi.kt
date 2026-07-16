package com.mileway.core.network.api.impl

import com.mileway.core.data.model.network.AllTaggedExpenseResponse
import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.BulkEventRequest
import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.data.model.network.BulkLocationRequest
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.DistanceResponseV2
import com.mileway.core.data.model.network.EmptyRequest
import com.mileway.core.data.model.network.EventRequest
import com.mileway.core.data.model.network.EventRequestV2
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LocationRequest
import com.mileway.core.data.model.network.LocationRequestV2
import com.mileway.core.data.model.network.LocationResponseV2
import com.mileway.core.data.model.network.LogMilesRequestV2
import com.mileway.core.data.model.network.LogMilesResponseV2
import com.mileway.core.data.model.network.LogMilesRoutesResponse
import com.mileway.core.data.model.network.LogMilesServicesResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.MapResponse
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.network.SubmittedCheckInResponseV2
import com.mileway.core.data.model.network.SuccessResponseV2
import com.mileway.core.data.model.network.TrackMileageStatusResponse
import com.mileway.core.network.api.MilewayNetworkApi
import com.siddharth.kmp.network.BaseUrlProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * PLAN_V33 A3: real Ktor-backed [MilewayNetworkApi] talking to `:server` (PLAN_V33 B1-B3). Selected
 * behind the `NetworkBackendFlags.useRealBackend` Koin flag in `:stub`'s StubModule — default is
 * `false`, so `FakeTrackingNetworkApi` stays the actual default and nothing changes unless the flag
 * is flipped.
 *
 * PLAN_V34 P2/A6: auth is wired now, but NOT inside this class — [client] is expected to already be
 * an authenticated client (`createHttpClient(...).withBearerAuth(tokenStore, authApi)`, see
 * `core.network.auth.BearerAuth`), so every `get`/`post` call below picks up the
 * `Authorization: Bearer` header and the 401 -> refresh -> retry-once flow for free. This class's
 * own call sites stay bare on purpose — the DI construction site (`:stub`'s `StubModule`) is the
 * only place that decides whether [client] is authenticated.
 *
 * Routes that already exist on `:server` (B1-B3): vehicles, pricing, miles/submit, location(/batch),
 * events(/batch), and the location/events GET range reads. Every other method below targets its
 * intended `/api/...` path per PLAN_V33.1's route list — a real HTTP call, not a stub — and is
 * marked "route pending B4" since the server doesn't serve it yet (it 404s until B4 lands).
 */
class KtorMilewayNetworkApi(
    private val client: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
) : MilewayNetworkApi {
    private suspend fun url(path: String): String = "${baseUrlProvider.baseUrl()}$path"

    override suspend fun vehicles(trackMiles: Boolean): PolicyApprovedVehiclesResponse =
        client.get(url("/api/vehicles")) { parameter("trackMiles", trackMiles) }.body()

    override suspend fun pricing(): ApprovedVehiclePricingResponse = client.get(url("/api/pricing")).body()

    // route pending B4
    override suspend fun submitMilesEvent(request: PostMileageEventRequestK) {
        client.post(url("/api/miles/event")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // route pending B4
    override suspend fun logMilesLimit(request: LogMilesRequestV2): LogMilesResponseV2 =
        client.post(url("/api/miles/log/limit")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun logMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse =
        client.post(url("/api/miles/log")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4
    override suspend fun fetchLogMilesServices(isInsideTrip: Boolean): LogMilesServicesResponse =
        client.get(url("/api/log-miles/services")) { parameter("insideTrip", isInsideTrip) }.body()

    // route pending B4
    override suspend fun logMilesRoutes(): LogMilesRoutesResponse = client.get(url("/api/log-miles/routes")).body()

    // route pending B4
    override suspend fun discardMiles(request: PostMileageEventRequestK) {
        client.post(url("/api/miles/discard")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // route pending B4
    override suspend fun distance(request: DistanceRequestV2): DistanceResponseV2 =
        client.post(url("/api/distance")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse =
        client.post(url("/api/miles/submit")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun getTrackMileageStatus(trackingToken: String): TrackMileageStatusResponse =
        client.get(url("/api/miles/status")) { parameter("token", trackingToken) }.body()

    // V1 legacy: LocationPayload is a strict field subset of the real V2 payload (no
    // provider/opId, both optional server-side), so this JSON is wire-compatible with the real
    // /api/location/batch route — a real call, not a stub.
    override suspend fun postLocation(request: BulkLocationRequest) {
        client.post(url("/api/location/batch")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postLocationSingle(request: LocationRequest) {
        client.post(url("/api/location")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postBulkEvents(request: BulkEventRequest) {
        client.post(url("/api/events/batch")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postEventSingle(request: EventRequest) {
        client.post(url("/api/events")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postLocationV2Single(request: LocationRequestV2) {
        client.post(url("/api/location")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postLocationV2Batch(request: BulkLocationRequestV2) {
        client.post(url("/api/location/batch")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postEventV2Single(request: EventRequestV2) {
        client.post(url("/api/events")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun postBulkEventsV2(request: BulkEventRequestV2) {
        client.post(url("/api/events/batch")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getLocationsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): LocationResponseV2 =
        client.get(url("/api/location")) {
            parameter("token", token)
            parameter("start", startTime)
            parameter("end", endTime)
        }.body()

    override suspend fun getEventsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): EventResponseV2 =
        client.get(url("/api/events")) {
            parameter("token", token)
            parameter("start", startTime)
            parameter("end", endTime)
        }.body()

    // route pending B4
    override suspend fun fetchMap(
        lat: String,
        lng: String,
    ): MapResponse =
        client.get(url("/api/map")) {
            parameter("lat", lat)
            parameter("lng", lng)
        }.body()

    // route pending B4
    override suspend fun geoTypeById(typeId: Long): CheckInDetailsResponseV2 = client.get(url("/api/checkin/types/$typeId")).body()

    // route pending B4
    override suspend fun submittedCheckins(token: String): SubmittedCheckInResponseV2 =
        client.get(url("/api/checkin/submitted")) { parameter("token", token) }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun geoTypes(): AllTypesResponseV2 = client.get(url("/api/checkin/types")).body()

    // route pending B4
    override suspend fun updateCenterLocation(request: CheckInRequestV2): SuccessResponseV2 =
        client.post(url("/api/checkin/center")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4
    override suspend fun resetMilesLocation(
        contactId: Long,
        request: EmptyRequest,
    ): SuccessResponseV2 =
        client.post(url("/api/miles/reset/$contactId")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun submitCheckIn(request: CheckInRequestV2): SuccessResponseV2 =
        client.post(url("/api/checkin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun allTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse =
        client.get(url("/api/expenses/tagged")) {
            parameter("start", start)
            parameter("end", end)
        }.body()

    // route pending B4 (path named in PLAN_V33.1's route list)
    override suspend fun pendingTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse =
        client.get(url("/api/expenses/pending")) {
            parameter("start", start)
            parameter("end", end)
        }.body()
}
