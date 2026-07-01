package com.mileway.core.network.api

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

interface MilewayNetworkApi {
    suspend fun vehicles(trackMiles: Boolean): PolicyApprovedVehiclesResponse

    suspend fun pricing(): ApprovedVehiclePricingResponse

    suspend fun submitMilesEvent(request: PostMileageEventRequestK)

    suspend fun logMilesLimit(request: LogMilesRequestV2): LogMilesResponseV2

    suspend fun logMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse

    suspend fun fetchLogMilesServices(isInsideTrip: Boolean): LogMilesServicesResponse

    suspend fun logMilesRoutes(): LogMilesRoutesResponse

    suspend fun discardMiles(request: PostMileageEventRequestK)

    suspend fun distance(request: DistanceRequestV2): DistanceResponseV2

    suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse

    suspend fun getTrackMileageStatus(trackingToken: String): TrackMileageStatusResponse

    suspend fun postLocation(request: BulkLocationRequest)

    suspend fun postLocationSingle(request: LocationRequest)

    suspend fun postBulkEvents(request: BulkEventRequest)

    suspend fun postEventSingle(request: EventRequest)

    suspend fun postLocationV2Single(request: LocationRequestV2)

    suspend fun postLocationV2Batch(request: BulkLocationRequestV2)

    suspend fun postEventV2Single(request: EventRequestV2)

    suspend fun postBulkEventsV2(request: BulkEventRequestV2)

    suspend fun getLocationsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): LocationResponseV2

    suspend fun getEventsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): EventResponseV2

    suspend fun fetchMap(
        lat: String,
        lng: String,
    ): MapResponse

    suspend fun geoTypeById(typeId: Long): CheckInDetailsResponseV2

    suspend fun submittedCheckins(token: String): SubmittedCheckInResponseV2

    suspend fun geoTypes(): AllTypesResponseV2

    suspend fun updateCenterLocation(request: CheckInRequestV2): SuccessResponseV2

    suspend fun resetMilesLocation(
        contactId: Long,
        request: EmptyRequest = EmptyRequest(),
    ): SuccessResponseV2

    suspend fun submitCheckIn(request: CheckInRequestV2): SuccessResponseV2

    suspend fun allTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse

    suspend fun pendingTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse
}
