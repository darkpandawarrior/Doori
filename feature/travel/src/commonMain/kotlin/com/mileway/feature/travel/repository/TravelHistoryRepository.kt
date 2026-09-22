package com.mileway.feature.travel.repository

import com.mileway.core.data.util.MillisPerDay
import com.mileway.feature.travel.model.BookingRequest
import com.mileway.feature.travel.model.BookingType
import com.mileway.feature.travel.model.TravelReqStatus
import com.mileway.feature.travel.model.TripRecord
import kotlin.time.Clock

/** First trip reference number; fixture ids read `TRP-4401`, `TRP-4402`, … */
private const val TripRefBase = 4_400

/** First booking reference number; fixture ids read `FLT-5001`, `HTL-5004`, … */
private const val BookingRefBase = 5_000

/**
 * Offline fake travel-history store (TR.8), a deterministic spread of submitted trip requests and booking
 * requests across all [TravelReqStatus]es / [BookingType]s, relative to a [Clock]-supplied `now` (no
 * `Math.random`). Backs the trip-history and booking-history surfaces; also the TR.9 `TravelSearchProvider`
 * source.
 */
class TravelHistoryRepository(
    private val clock: Clock = Clock.System,
) {
    private fun trip(
        index: Int,
        purpose: String,
        route: String,
        status: TravelReqStatus,
        daysAgo: Long,
    ): TripRecord {
        val now = clock.now().toEpochMilliseconds()
        return TripRecord("TRP-${TripRefBase + index}", purpose, route, status, now - daysAgo * MillisPerDay)
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
        return BookingRequest(
            "$prefix-${BookingRefBase + index}",
            type,
            summary,
            status,
            amount,
            now - daysAgo * MillisPerDay,
        )
    }

    // Fixture rows are written with named arguments: `daysAgo = 21L` and `amount = 7800.0` say what the
    // number is at the only place a reader meets it, which is what extracting a constant per row would
    // otherwise have to do twenty-two times over.
    private fun allTrips(): List<TripRecord> =
        listOf(
            trip(index = 1, purpose = "Client visit", route = "Pune → Mumbai", status = TravelReqStatus.PENDING, daysAgo = 1L),
            trip(index = 2, purpose = "Conference", route = "Pune → Delhi", status = TravelReqStatus.APPROVED, daysAgo = 6L),
            trip(index = 3, purpose = "Site audit", route = "Pune → Bengaluru", status = TravelReqStatus.COMPLETED, daysAgo = 21L),
            trip(index = 4, purpose = "Vendor meet", route = "Mumbai → Chennai", status = TravelReqStatus.REJECTED, daysAgo = 14L),
        )

    private fun allBookings(): List<BookingRequest> =
        listOf(
            booking(
                index = 1,
                type = BookingType.FLIGHT,
                summary = "PNQ → DEL · IndiGo",
                status = TravelReqStatus.PENDING,
                amount = 7800.0,
                daysAgo = 2L,
            ),
            booking(
                index = 2,
                type = BookingType.FLIGHT,
                summary = "BOM → BLR · Air India",
                status = TravelReqStatus.APPROVED,
                amount = 6200.0,
                daysAgo = 9L,
            ),
            booking(
                index = 3,
                type = BookingType.BUS,
                summary = "PNQ → Goa · sleeper",
                status = TravelReqStatus.COMPLETED,
                amount = 1400.0,
                daysAgo = 18L,
            ),
            booking(
                index = 4,
                type = BookingType.HOTEL,
                summary = "Trident BKC · 3 nights",
                status = TravelReqStatus.APPROVED,
                amount = 21_000.0,
                daysAgo = 5L,
            ),
            booking(
                index = 5,
                type = BookingType.MJP,
                summary = "Pune → Delhi → Jaipur",
                status = TravelReqStatus.PENDING,
                amount = null,
                daysAgo = 3L,
            ),
            booking(
                index = 6,
                type = BookingType.VISA,
                summary = "Singapore · Business",
                status = TravelReqStatus.REJECTED,
                amount = null,
                daysAgo = 12L,
            ),
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
