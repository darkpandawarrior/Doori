package com.mileway

import com.mileway.feature.travel.model.TransportMode
import com.mileway.feature.travel.model.TripStatus
import com.mileway.feature.travel.repository.TravelRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TravelTest {

    private val repo = TravelRepository()

    @Test
    fun `bookings has 4 deterministic entries`() {
        assertEquals(4, repo.bookings.size)
    }

    @Test
    fun `ids are unique`() {
        val ids = repo.bookings.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `exactly one booking is active`() {
        val active = repo.bookings.filter { it.status == TripStatus.ACTIVE }
        assertEquals(1, active.size)
    }

    @Test
    fun `active booking is PNQ to BOM IndiGo flight`() {
        val active = repo.activeBooking()
        assertNotNull(active)
        assertEquals("PNQ", active.origin)
        assertEquals("BOM", active.destination)
        assertEquals(TransportMode.FLIGHT, active.mode)
        assertEquals("IndiGo", active.carrier)
        assertEquals("6E-401", active.flightOrTrainNumber)
        assertEquals("B7", active.gate)
        assertEquals("14:30", active.boardingTime)
    }

    @Test
    fun `upcoming count is 3`() {
        assertEquals(3, repo.upcomingBookings().size)
    }

    @Test
    fun `all upcoming bookings have UPCOMING status`() {
        repo.upcomingBookings().forEach { booking ->
            assertEquals(TripStatus.UPCOMING, booking.status)
        }
    }

    @Test
    fun `upcoming bookings include BOM-DEL flight`() {
        val found = repo.upcomingBookings().any {
            it.origin == "BOM" && it.destination == "DEL" && it.carrier == "Air India"
        }
        assertTrue(found)
    }

    @Test
    fun `upcoming bookings include PNQ-BLR train`() {
        val found = repo.upcomingBookings().any {
            it.origin == "PNQ" && it.destination == "BLR" && it.mode == TransportMode.TRAIN
        }
        assertTrue(found)
    }

    @Test
    fun `upcoming bookings include DEL-PNQ IndiGo flight`() {
        val found = repo.upcomingBookings().any {
            it.origin == "DEL" && it.destination == "PNQ" && it.carrier == "IndiGo"
        }
        assertTrue(found)
    }

    @Test
    fun `all bookings have positive amounts`() {
        repo.bookings.forEach { booking ->
            assertTrue(booking.amountRupees > 0.0, "Expected positive amount for ${booking.id}")
        }
    }

    @Test
    fun `all departure timestamps use deterministic base`() {
        val baseMs = 1_700_000_000_000L
        repo.bookings.forEach { booking ->
            assertTrue(booking.departureMs >= baseMs, "Expected departure >= baseMs for ${booking.id}")
        }
    }

    @Test
    fun `active booking has gate and boarding time`() {
        val active = repo.activeBooking()
        assertNotNull(active)
        assertNotNull(active.gate)
        assertNotNull(active.boardingTime)
    }

    @Test
    fun `upcoming bookings have no gate or boarding time`() {
        repo.upcomingBookings().forEach { booking ->
            assertEquals(null, booking.gate)
            assertEquals(null, booking.boardingTime)
        }
    }

    @Test
    fun `total spend across all bookings is positive`() {
        val total = repo.bookings.sumOf { it.amountRupees }
        assertTrue(total > 0.0)
    }
}
