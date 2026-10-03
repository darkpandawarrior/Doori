package com.mileway.feature.tracking.claim

import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.PolicyVersion
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.core.data.model.db.SavedTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TripToClaimMapperTest {
    private val mapper =
        TripToClaimMapper(
            PolicyEngine(
                listOf(
                    PolicyVersion(0L, PolicyRateTable(mapOf("car" to 1_200.0))),
                    PolicyVersion(10_000L, PolicyRateTable(mapOf("car" to 2_000.0), maxReimbursement = 15_000.0)),
                ),
            ),
        )

    @Test
    fun `completed trip maps metres to kilometres and prices at its completion date`() {
        val line = requireNotNull(mapper.map(completedTrip()))
        assertEquals("trip-1", line.sourceTripId)
        assertEquals(10.0, line.distanceKm)
        assertEquals(12_000L, line.amountMinor)
        assertEquals("car", line.vehicleKey)
        assertEquals("INR", line.currency)
        assertEquals(emptyList(), line.policyFlags)

        val capped = requireNotNull(mapper.map(completedTrip().copy(endTime = 20_000L)))
        assertEquals(15_000L, capped.amountMinor)
        assertEquals(listOf("MILEAGE_RATE_CAPPED"), capped.policyFlags)

        // A completed drive with no movement is still recorded once, with nothing reimbursable.
        assertEquals(0L, requireNotNull(mapper.map(completedTrip().copy(distance = 0.0))).amountMinor)
    }

    @Test
    fun `unfinished discarded simulated unowned and invalid trips do not become claim lines`() {
        val trip = completedTrip()
        listOf(
            trip.copy(isCompleted = false),
            trip.copy(isDiscarded = true),
            trip.copy(wasMockOn = true),
            trip.copy(wasMockLocationUsed = true),
            trip.copy(startedByEmployeeCode = ""),
            trip.copy(distance = -1.0),
            trip.copy(distance = Double.NaN),
            trip.copy(distance = Double.POSITIVE_INFINITY),
            trip.copy(endTime = 0L),
        ).forEach { assertNull(mapper.map(it)) }
    }
}

internal fun completedTrip(): SavedTrack =
    SavedTrack(
        routeId = "trip-1",
        name = "Tracked drive",
        isCompleted = true,
        startedByEmployeeCode = "emp-1",
        startLatitude = 18.0,
        startLongitude = 73.0,
        endLatitude = 18.1,
        endLongitude = 73.1,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 1_000L,
        endTime = 2_000L,
        distance = 10_000.0,
        duration = 1_000L,
        selectedVehicleType = "car",
    )
