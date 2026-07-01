package com.mileway.feature.travel.model

/** Lifecycle status shared by trip requests and booking requests (TR.8). */
enum class TravelReqStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
}

/** The booking-request families surfaced in the booking history (TR.8). */
enum class BookingType(val label: String) {
    FLIGHT("Flight"),
    BUS("Bus"),
    HOTEL("Hotel"),
    MJP("MJP"),
    VISA("Visa"),
}

/** One submitted trip request in the trip history (TR.8). */
data class TripRecord(
    val id: String,
    val purpose: String,
    val route: String,
    val status: TravelReqStatus,
    val dateMillis: Long,
)

/** One submitted booking request in the booking history (TR.8). */
data class BookingRequest(
    val id: String,
    val type: BookingType,
    val summary: String,
    val status: TravelReqStatus,
    val amount: Double?,
    val dateMillis: Long,
)
