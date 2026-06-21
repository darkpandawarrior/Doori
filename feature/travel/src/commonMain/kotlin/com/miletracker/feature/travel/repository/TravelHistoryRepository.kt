package com.miletracker.feature.travel.repository

import com.miletracker.feature.travel.model.BookingRequest
import com.miletracker.feature.travel.model.BookingType
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.model.TripRecord
import kotlin.time.Clock

/**
 * Offline fake travel-history store (TR.8), a deterministic spread of submitted trip requests and booking
 * requests across all [TravelReqStatus]es / [BookingType]s, relative to a [Clock]-supplied `now` (no
 * `Math.random`). Backs the trip-history and booking-history surfaces; also the TR.9 `TravelSearchProvider`
 * source.
 */
class TravelHistoryRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L

    private fun trip(
        index: Int,
        purpose: String,
        route: String,
        status: TravelReqStatus,
        daysAgo: Long,
    ): TripRecord {
        val now = clock.now().toEpochMilliseconds()
        return TripRecord("TRP-${4400 + index}", purpose, route, status, now - daysAgo * dayMs)
    }

    private fun booking(
        index: Int,
        type: BookingType,
        summary: String,
        status: TravelReqStatus,
        amount: Double?,
        daysAgo: Long,
    ): BookingRequest {
        val now = clock.now().toEpochMilliseconds()
        val prefix =
            when (type) {
                BookingType.FLIGHT -> "FLT"
                BookingType.BUS -> "BUS"
                BookingType.HOTEL -> "HTL"
                BookingType.MJP -> "MJP"
                BookingType.VISA -> "VSA"
            }
        return BookingRequest("$prefix-${5000 + index}", type, summary, status, amount, now - daysAgo * dayMs)
    }

    private fun allTrips(): List<TripRecord> =
        listOf(
            trip(1, "Client visit", "Pune → Mumbai", TravelReqStatus.PENDING, 1L),
            trip(2, "Conference", "Pune → Delhi", TravelReqStatus.APPROVED, 6L),
            trip(3, "Site audit", "Pune → Bengaluru", TravelReqStatus.COMPLETED, 21L),
            trip(4, "Vendor meet", "Mumbai → Chennai", TravelReqStatus.REJECTED, 14L),
        )

    private fun allBookings(): List<BookingRequest> =
        listOf(
            booking(1, BookingType.FLIGHT, "PNQ → DEL · IndiGo", TravelReqStatus.PENDING, 7800.0, 2L),
            booking(2, BookingType.FLIGHT, "BOM → BLR · Air India", TravelReqStatus.APPROVED, 6200.0, 9L),
            booking(3, BookingType.BUS, "PNQ → Goa · sleeper", TravelReqStatus.COMPLETED, 1400.0, 18L),
            booking(4, BookingType.HOTEL, "Trident BKC · 3 nights", TravelReqStatus.APPROVED, 21_000.0, 5L),
            booking(5, BookingType.MJP, "Pune → Delhi → Jaipur", TravelReqStatus.PENDING, null, 3L),
            booking(6, BookingType.VISA, "Singapore · Business", TravelReqStatus.REJECTED, null, 12L),
        )

    /** All trips, or just those in [status] when non-null, newest first. */
    fun trips(status: TravelReqStatus? = null): List<TripRecord> =
        allTrips().filter { status == null || it.status == status }.sortedByDescending { it.dateMillis }

    /** All bookings, optionally narrowed to [type] and/or [status], newest first. */
    fun bookings(
        type: BookingType? = null,
        status: TravelReqStatus? = null,
    ): List<BookingRequest> =
        allBookings()
            .filter { (type == null || it.type == type) && (status == null || it.status == status) }
            .sortedByDescending { it.dateMillis }
}
