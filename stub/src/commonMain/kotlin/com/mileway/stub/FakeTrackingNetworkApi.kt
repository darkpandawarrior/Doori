package com.mileway.stub

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

class FakeTrackingNetworkApi : MilewayNetworkApi {
    override suspend fun vehicles(trackMiles: Boolean): PolicyApprovedVehiclesResponse = DemoMockData.vehicles(trackMiles)

    override suspend fun pricing(): ApprovedVehiclePricingResponse = ApprovedVehiclePricingResponse()

    override suspend fun submitMilesEvent(request: PostMileageEventRequestK) { /* no-op */ }

    override suspend fun logMilesLimit(request: LogMilesRequestV2): LogMilesResponseV2 = LogMilesResponseV2()

    override suspend fun logMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse =
        PolicyMockData.enrich(
            base = DemoMockData.submissionResponse(request.distance),
            distanceKm = request.distance,
            token = request.vehicleType,
        )

    override suspend fun fetchLogMilesServices(isInsideTrip: Boolean): LogMilesServicesResponse =
        DemoMockData.logMilesServices(isInsideTrip)

    override suspend fun logMilesRoutes(): LogMilesRoutesResponse = DemoMockData.logMilesRoutes()

    override suspend fun discardMiles(request: PostMileageEventRequestK) { /* no-op */ }

    override suspend fun distance(request: DistanceRequestV2): DistanceResponseV2 {
        val coords = request.coords
        if (coords.size < 2) return DistanceResponseV2(0.0)
        var totalKm = 0.0
        for (i in 0 until coords.size - 1) {
            val a = coords[i]
            val b = coords[i + 1]
            if (a.lat != null && a.lng != null && b.lat != null && b.lng != null) {
                totalKm += haversineKm(a.lat!!, a.lng!!, b.lat!!, b.lng!!)
            }
        }
        return DistanceResponseV2(distance = totalKm)
    }

    override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse =
        PolicyMockData.enrich(
            base = DemoMockData.submissionResponse(request.distance),
            distanceKm = request.distance,
            token = request.token,
        )

    override suspend fun getTrackMileageStatus(trackingToken: String): TrackMileageStatusResponse = DemoMockData.trackMileageStatus()

    override suspend fun postLocation(request: BulkLocationRequest) { /* no-op */ }

    override suspend fun postLocationSingle(request: LocationRequest) { /* no-op */ }

    override suspend fun postBulkEvents(request: BulkEventRequest) { /* no-op */ }

    override suspend fun postEventSingle(request: EventRequest) { /* no-op */ }

    override suspend fun postLocationV2Single(request: LocationRequestV2) { /* no-op */ }

    override suspend fun postLocationV2Batch(request: BulkLocationRequestV2) { /* no-op */ }

    override suspend fun postEventV2Single(request: EventRequestV2) { /* no-op */ }

    override suspend fun postBulkEventsV2(request: BulkEventRequestV2) { /* no-op */ }

    override suspend fun getLocationsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): LocationResponseV2 = LocationResponseV2()

    override suspend fun getEventsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): EventResponseV2 = EventResponseV2()

    override suspend fun fetchMap(
        lat: String,
        lng: String,
    ): MapResponse = MapResponse(address = "Demo Location", lat = lat.toDoubleOrNull(), lng = lng.toDoubleOrNull())

    override suspend fun geoTypeById(typeId: Long): CheckInDetailsResponseV2 =
        CheckInDetailsResponseV2(id = typeId, name = "Office", radius = 200.0)

    override suspend fun submittedCheckins(token: String): SubmittedCheckInResponseV2 = SubmittedCheckInResponseV2()

    override suspend fun geoTypes(): AllTypesResponseV2 = AllTypesResponseV2()

    override suspend fun updateCenterLocation(request: CheckInRequestV2): SuccessResponseV2 = SuccessResponseV2()

    override suspend fun resetMilesLocation(
        contactId: Long,
        request: EmptyRequest,
    ): SuccessResponseV2 = SuccessResponseV2()

    override suspend fun submitCheckIn(request: CheckInRequestV2): SuccessResponseV2 = SuccessResponseV2()

    override suspend fun allTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = AllTaggedExpenseResponse()

    override suspend fun pendingTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = AllTaggedExpenseResponse()

    private fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
        val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
        val a =
            kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
                kotlin.math.sin(dLon / 2).let { it * it }
        return earthRadiusKm * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}
