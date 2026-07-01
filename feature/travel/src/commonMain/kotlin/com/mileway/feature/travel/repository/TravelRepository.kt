package com.mileway.feature.travel.repository

import com.mileway.feature.travel.model.BookingRecord
import com.mileway.feature.travel.model.TransportMode
import com.mileway.feature.travel.model.TripStatus

private val BASE_MS = 1_781_654_400_000L
private val DAY_MS = 86_400_000L

class TravelRepository {
    val bookings: List<BookingRecord> =
        listOf(
            BookingRecord(
                id = "BK001",
                mode = TransportMode.FLIGHT,
                origin = "PNQ",
                destination = "BOM",
                carrier = "IndiGo",
                flightOrTrainNumber = "6E-401",
                departureMs = BASE_MS + DAY_MS,
                amountRupees = 3600.0,
                status = TripStatus.ACTIVE,
                gate = "B7",
                boardingTime = "14:30",
            ),
            BookingRecord(
                id = "BK002",
                mode = TransportMode.FLIGHT,
                origin = "BOM",
                destination = "DEL",
                carrier = "Air India",
                flightOrTrainNumber = "AI-864",
                departureMs = BASE_MS + 22 * DAY_MS,
                amountRupees = 7800.0,
                status = TripStatus.UPCOMING,
            ),
            BookingRecord(
                id = "BK003",
                mode = TransportMode.TRAIN,
                origin = "PNQ",
                destination = "BLR",
                carrier = "Indian Railways",
                flightOrTrainNumber = "Deccan Queen",
                departureMs = BASE_MS + 28 * DAY_MS,
                amountRupees = 1200.0,
                status = TripStatus.UPCOMING,
            ),
            BookingRecord(
                id = "BK004",
                mode = TransportMode.FLIGHT,
                origin = "DEL",
                destination = "PNQ",
                carrier = "IndiGo",
                flightOrTrainNumber = "6E-712",
                departureMs = BASE_MS + 35 * DAY_MS,
                amountRupees = 4900.0,
                status = TripStatus.UPCOMING,
            ),
        )

    fun activeBooking(): BookingRecord? = bookings.firstOrNull { it.status == TripStatus.ACTIVE }

    fun upcomingBookings(): List<BookingRecord> = bookings.filter { it.status == TripStatus.UPCOMING }

    /** Total illustrative travel spend across all bookings (TR.1, replaces the old `:stub` AnalyticsMockData read). */
    fun totalSpend(): Double = bookings.sumOf { it.amountRupees }
}
