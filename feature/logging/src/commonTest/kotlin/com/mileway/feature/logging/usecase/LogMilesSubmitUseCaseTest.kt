package com.mileway.feature.logging.usecase

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
import com.siddharth.kmp.offlineoutbox.DraftEntry
import com.siddharth.kmp.offlineoutbox.DraftStatus
import com.siddharth.kmp.offlineoutbox.SubmitOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Network fake — only [logMiles] is exercised by [LogMilesSubmitUseCase]; [shouldFail] drives the failure path. */
private class FakeLogMilesApi(private val shouldFail: Boolean = false) : MilewayNetworkApi {
    override suspend fun vehicles(trackMiles: Boolean): PolicyApprovedVehiclesResponse = error("unused")

    override suspend fun pricing(): ApprovedVehiclePricingResponse = error("unused")

    override suspend fun submitMilesEvent(request: PostMileageEventRequestK) = error("unused")

    override suspend fun logMilesLimit(request: LogMilesRequestV2): LogMilesResponseV2 = error("unused")

    override suspend fun logMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse =
        if (shouldFail) throw IllegalStateException("network down") else ExpenseSubmissionResponse()

    override suspend fun fetchLogMilesServices(isInsideTrip: Boolean): LogMilesServicesResponse = error("unused")

    override suspend fun logMilesRoutes(): LogMilesRoutesResponse = error("unused")

    override suspend fun discardMiles(request: PostMileageEventRequestK) = error("unused")

    override suspend fun distance(request: DistanceRequestV2): DistanceResponseV2 = error("unused")

    override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse = error("unused")

    override suspend fun getTrackMileageStatus(trackingToken: String): TrackMileageStatusResponse = error("unused")

    override suspend fun postLocation(request: BulkLocationRequest) = error("unused")

    override suspend fun postLocationSingle(request: LocationRequest) = error("unused")

    override suspend fun postBulkEvents(request: BulkEventRequest) = error("unused")

    override suspend fun postEventSingle(request: EventRequest) = error("unused")

    override suspend fun postLocationV2Single(request: LocationRequestV2) = error("unused")

    override suspend fun postLocationV2Batch(request: BulkLocationRequestV2) = error("unused")

    override suspend fun postEventV2Single(request: EventRequestV2) = error("unused")

    override suspend fun postBulkEventsV2(request: BulkEventRequestV2) = error("unused")

    override suspend fun getLocationsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): LocationResponseV2 = error("unused")

    override suspend fun getEventsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): EventResponseV2 = error("unused")

    override suspend fun fetchMap(
        lat: String,
        lng: String,
    ): MapResponse = error("unused")

    override suspend fun geoTypeById(typeId: Long): CheckInDetailsResponseV2 = error("unused")

    override suspend fun submittedCheckins(token: String): SubmittedCheckInResponseV2 = error("unused")

    override suspend fun geoTypes(): AllTypesResponseV2 = error("unused")

    override suspend fun updateCenterLocation(request: CheckInRequestV2): SuccessResponseV2 = error("unused")

    override suspend fun resetMilesLocation(
        contactId: Long,
        request: EmptyRequest,
    ): SuccessResponseV2 = error("unused")

    override suspend fun submitCheckIn(request: CheckInRequestV2): SuccessResponseV2 = error("unused")

    override suspend fun allTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = error("unused")

    override suspend fun pendingTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = error("unused")
}

/** In-memory [SubmitOutbox] — enough to assert enqueue/mark calls without a Room DB. */
private class FakeSubmitOutbox<T : Any> : SubmitOutbox<T> {
    private val entries = MutableStateFlow<Map<String, DraftEntry<T>>>(emptyMap())

    val snapshot: List<DraftEntry<T>> get() = entries.value.values.toList()

    override fun drafts(formKey: String): Flow<List<DraftEntry<T>>> = entries.map { it.values.filter { e -> e.formKey == formKey } }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: T,
    ) {
        entries.value =
            entries.value +
            (
                uniqueKey to
                    DraftEntry(formKey, uniqueKey, payload, DraftStatus.PENDING, null, 0L, 0L)
            )
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
            entries.value +
            (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.FAILED, errorMessage = error))
    }

    override suspend fun clear(formKey: String) {
        entries.value = entries.value.filterValues { it.formKey != formKey }
    }
}

/**
 * Wave 3 acceptance: a Log Miles submission is durable — [LogMilesSubmitUseCase] enqueues into
 * [SubmitOutbox] before calling the network, then marks SUBMITTED/FAILED once the call resolves,
 * so a manual entry survives process death between enqueue and network response.
 */
class LogMilesSubmitUseCaseTest {
    private val request = LogMilesSubmitRequestV2(vehicleType = "car", distance = 10.0)

    @Test
    fun `a successful submit enqueues then marks the outbox entry SUBMITTED`() =
        runTest {
            val outbox = FakeSubmitOutbox<LogMilesSubmitRequestV2>()
            val useCase = LogMilesSubmitUseCase(FakeLogMilesApi(), outbox)

            val result = useCase(request)

            assertTrue(result.isSuccess)
            assertEquals(1, outbox.snapshot.size)
            assertEquals(DraftStatus.SUBMITTED, outbox.snapshot.single().status)
            assertEquals(LogMilesSubmitUseCase.FORM_KEY, outbox.snapshot.single().formKey)
        }

    @Test
    fun `a failed submit marks the outbox entry FAILED with the error message`() =
        runTest {
            val outbox = FakeSubmitOutbox<LogMilesSubmitRequestV2>()
            val useCase = LogMilesSubmitUseCase(FakeLogMilesApi(shouldFail = true), outbox)

            val result = useCase(request)

            assertTrue(result.isFailure)
            assertEquals(DraftStatus.FAILED, outbox.snapshot.single().status)
            assertEquals("network down", outbox.snapshot.single().errorMessage)
        }
}
