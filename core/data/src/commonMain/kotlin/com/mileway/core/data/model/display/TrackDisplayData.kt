package com.mileway.core.data.model.display

import androidx.compose.runtime.Immutable
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.fmt2d

@Immutable
data class TrackDisplayData(
    val token: String,
    val name: String? = null,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val distanceKm: Double = 0.0,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val selectedVehicleType: String = "",
    val vehiclePricing: Double = 0.0,
    val service: String = "",
    val avgSpeedKmh: Double = 0.0,
    val reimbursableAmount: Double = 0.0,
    val submittedAt: Long = 0L,
    val isSubmitted: Boolean = false,
    val locationCount: Int = 0,
    // P3.3: non-null once this trip has been selected into a submitted voucher — the
    // already-claimed guard excludes these from a subsequent Create Voucher selection list.
    val claimedByVoucherNumber: String? = null,
    // Wave 3 linked-context card: carried through unmapped so the hub can render it without a
    // second SavedTrack lookup. See feature:tracking's LinkedContext for the source fields.
    val tripId: String? = null,
    val tripV2Id: String? = null,
    val itineraryId: String? = null,
    val pettyId: Long = -1L,
) {
    fun getDurationMs(): Long =
        when {
            endTime > 0 && startTime > 0 -> endTime - startTime
            startTime > 0 -> kotlin.time.Clock.System.now().toEpochMilliseconds() - startTime
            else -> 0L
        }

    fun getFormattedDuration(): String {
        val ms = getDurationMs()
        val minutes = (ms / 60_000) % 60
        val hours = ms / 3_600_000
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun getFormattedDistance(): String = "${distanceKm.fmt2d()} km"

    fun getTrackingState(): TrackingState =
        when {
            isTracking && isPaused -> TrackingState.PAUSED
            isTracking -> TrackingState.LIVE_TRACKING
            startTime > 0 && endTime > 0 -> TrackingState.COMPLETED
            else -> TrackingState.READY
        }
}

fun SavedTrack.toDisplayData() =
    TrackDisplayData(
        token = routeId,
        name = name,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        startTime = startTime,
        endTime = endTime,
        distanceKm = distance / 1000.0,
        selectedVehicleType = selectedVehicleType,
        vehiclePricing = vehiclePricing,
        service = service,
        reimbursableAmount = submittedAmount,
        submittedAt = submissionTime,
        isSubmitted = serverUploaded,
        locationCount = totalLocationPoints.toInt(),
        claimedByVoucherNumber = claimedByVoucherNumber,
        tripId = tripId,
        tripV2Id = tripV2Id,
        itineraryId = itineraryId,
        pettyId = pettyId,
    )
