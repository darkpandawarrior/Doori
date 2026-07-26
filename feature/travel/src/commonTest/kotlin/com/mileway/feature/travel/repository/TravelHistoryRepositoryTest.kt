package com.mileway.feature.travel.repository

import com.mileway.feature.travel.model.BookingType
import com.mileway.feature.travel.model.TravelReqStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private const val NOW_MS = 1_700_000_000_000L

private class FixedClock(private val ms: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
}

/** TR.8: covers [TravelHistoryRepository] — trip/booking status & type filters and newest-first sort. */
class TravelHistoryRepositoryTest {
    private fun repo() = TravelHistoryRepository(FixedClock(NOW_MS))

    @Test
    fun `trips returns all four seeded trips sorted newest first`() {
        val trips = repo().trips()

        assertEquals(4, trips.size)
        assertEquals(listOf("TRP-4401", "TRP-4402", "TRP-4404", "TRP-4403"), trips.map { it.id })
        assertTrue(trips.zipWithNext().all { (a, b) -> a.dateMillis >= b.dateMillis })
    }

    @Test
    fun `trips filters by status`() {
        val completed = repo().trips(status = TravelReqStatus.COMPLETED)

        assertEquals(listOf("TRP-4403"), completed.map { it.id })
    }

    @Test
    fun `trips filters by a different status value than the completed case above`() {
        val approved = repo().trips(status = TravelReqStatus.APPROVED)

        assertEquals(listOf("TRP-4402"), approved.map { it.id })
    }

    @Test
    fun `bookings returns all six seeded bookings sorted newest first`() {
        val bookings = repo().bookings()

        assertEquals(6, bookings.size)
        assertEquals(
            listOf("FLT-5001", "MJP-5005", "HTL-5004", "FLT-5002", "VSA-5006", "BUS-5003"),
            bookings.map { it.id },
        )
    }

    @Test
    fun `bookings filters by type`() {
        val flights = repo().bookings(type = BookingType.FLIGHT)

        assertTrue(flights.all { it.type == BookingType.FLIGHT })
        assertEquals(listOf("FLT-5001", "FLT-5002"), flights.map { it.id })
    }

    @Test
    fun `bookings filters by type and status combined returns empty when the combo does not exist`() {
        // HTL-5004 is the only HOTEL booking and it is APPROVED, never PENDING.
        val result = repo().bookings(type = BookingType.HOTEL, status = TravelReqStatus.PENDING)

        assertTrue(result.isEmpty())
    }
}
