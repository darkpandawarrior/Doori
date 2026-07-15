package com.mileway.core.data.model.validator

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.SavedTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JourneyValidatorTest {
    private val now = 1_700_000_000_000L

    private fun track(
        routeId: String = "route-1",
        isCompleted: Boolean = false,
        serverUploaded: Boolean = false,
        transId: String? = null,
        startTime: Long = now - 60_000L,
        endTime: Long = now,
        startLatitude: Double = 12.0,
        startLongitude: Double = 77.0,
        distance: Double = 1_000.0,
        selectedVehicleType: String = "CAR",
        lastSyncedTime: Long = now,
    ) = SavedTrack(
        routeId = routeId,
        name = "Trip",
        isCompleted = isCompleted,
        serverUploaded = serverUploaded,
        transId = transId,
        startTime = startTime,
        endTime = endTime,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = startLatitude,
        endLongitude = startLongitude,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        distance = distance,
        duration = endTime - startTime,
        selectedVehicleType = selectedVehicleType,
        lastSyncedTime = lastSyncedTime,
    )

    private fun current(
        token: String = "token-1",
        isTracking: Boolean = false,
        distance: Double = 1_000.0,
        lastSyncedTime: Long = now,
    ) = CurrentTrackData(token = token, isTracking = isTracking, distance = distance, lastSyncedTime = lastSyncedTime)

    // -- canSubmit --------------------------------------------------------

    @Test
    fun `canSubmit true for a ready-to-submit track`() {
        assertTrue(JourneyValidator.canSubmit(track()))
    }

    @Test
    fun `canSubmit false once server-uploaded`() {
        assertFalse(JourneyValidator.canSubmit(track(serverUploaded = true)))
    }

    @Test
    fun `canSubmit false without a start time`() {
        assertFalse(JourneyValidator.canSubmit(track(startTime = 0L)))
    }

    @Test
    fun `canSubmit false without a vehicle type`() {
        assertFalse(JourneyValidator.canSubmit(track(selectedVehicleType = "NONE")))
    }

    // -- validateBeforeSubmission -----------------------------------------

    @Test
    fun `validateBeforeSubmission valid when everything lines up`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(), nowMs = now)
        assertTrue(result.isValid)
        assertFalse(result.hasBlockers())
    }

    @Test
    fun `validateBeforeSubmission blank token is INVALID_TOKEN`() {
        val result = JourneyValidator.validateBeforeSubmission(current(token = ""), track(), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_TOKEN && it.severity == JourneySeverity.ERROR })
    }

    @Test
    fun `validateBeforeSubmission ALREADY_SUBMITTED when saved track already uploaded`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(serverUploaded = true), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.ALREADY_SUBMITTED })
        assertTrue(result.hasBlockers())
    }

    @Test
    fun `validateBeforeSubmission ALREADY_COMPLETED when completed with a transId`() {
        val result =
            JourneyValidator.validateBeforeSubmission(current(), track(isCompleted = true, transId = "txn-1"), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.ALREADY_COMPLETED })
    }

    @Test
    fun `validateBeforeSubmission INVALID_TIME_RANGE for a zero start time`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(startTime = 0L), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_TIME_RANGE && it.severity == JourneySeverity.ERROR })
    }

    @Test
    fun `validateBeforeSubmission INVALID_TIME_RANGE when start is more than 24h in the future`() {
        val farFuture = now + 25 * 60 * 60 * 1000L
        val result = JourneyValidator.validateBeforeSubmission(current(), track(startTime = farFuture, endTime = farFuture), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_TIME_RANGE && it.severity == JourneySeverity.ERROR })
    }

    @Test
    fun `validateBeforeSubmission slight clock drift within 24h buffer is only a warning`() {
        val nearFuture = now + 60_000L
        val result = JourneyValidator.validateBeforeSubmission(current(), track(startTime = nearFuture, endTime = nearFuture), nowMs = now)
        assertFalse(result.errors.any { it.code == JourneyErrorCode.INVALID_TIME_RANGE })
        assertTrue(result.errors.any { it.code == JourneyErrorCode.STALE_DATA && it.severity == JourneySeverity.WARNING })
        assertTrue(result.isValid) // warning only, not a blocker
    }

    @Test
    fun `validateBeforeSubmission INVALID_TIME_RANGE when end precedes start`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(startTime = now, endTime = now - 10_000L), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_TIME_RANGE })
    }

    @Test
    fun `validateBeforeSubmission INVALID_LOCATION for out-of-bounds coordinates`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(startLatitude = 200.0), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_LOCATION })
    }

    @Test
    fun `validateBeforeSubmission DATA_DISCREPANCY when distances diverge over 10 percent`() {
        val result =
            JourneyValidator.validateBeforeSubmission(current(distance = 1_500.0), track(distance = 1_000.0), nowMs = now)
        val discrepancy = result.errors.single { it.code == JourneyErrorCode.DATA_DISCREPANCY }
        assertEquals(JourneySeverity.WARNING, discrepancy.severity)
    }

    @Test
    fun `validateBeforeSubmission no DATA_DISCREPANCY within 10 percent`() {
        val result =
            JourneyValidator.validateBeforeSubmission(current(distance = 1_050.0), track(distance = 1_000.0), nowMs = now)
        assertFalse(result.errors.any { it.code == JourneyErrorCode.DATA_DISCREPANCY })
    }

    @Test
    fun `validateBeforeSubmission STALE_DATA when saved track synced more recently than DataStore`() {
        val result =
            JourneyValidator.validateBeforeSubmission(
                current(lastSyncedTime = now - 5_000L),
                track(lastSyncedTime = now),
                nowMs = now,
            )
        assertTrue(result.errors.any { it.code == JourneyErrorCode.STALE_DATA })
    }

    @Test
    fun `validateBeforeSubmission MISSING_CRITICAL_DATA without a vehicle type`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), track(selectedVehicleType = "NONE"), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.MISSING_CRITICAL_DATA && it.severity == JourneySeverity.ERROR })
        assertFalse(result.isValid)
    }

    @Test
    fun `validateBeforeSubmission with no saved track only checks the token`() {
        val result = JourneyValidator.validateBeforeSubmission(current(), savedTrack = null, nowMs = now)
        assertTrue(result.isValid)
    }

    // -- validateForRestoration --------------------------------------------

    @Test
    fun `validateForRestoration valid for a fresh ongoing track`() {
        val result = JourneyValidator.validateForRestoration(track(), nowMs = now)
        assertTrue(result.isValid)
    }

    @Test
    fun `validateForRestoration blocks a completed track`() {
        val result = JourneyValidator.validateForRestoration(track(isCompleted = true), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.ALREADY_COMPLETED })
        assertFalse(result.isValid)
    }

    @Test
    fun `validateForRestoration blocks a server-uploaded track`() {
        val result = JourneyValidator.validateForRestoration(track(serverUploaded = true), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.ALREADY_SUBMITTED })
    }

    @Test
    fun `validateForRestoration blocks an invalid start time`() {
        val result = JourneyValidator.validateForRestoration(track(startTime = 0L), nowMs = now)
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INVALID_TIME_RANGE })
    }

    @Test
    fun `validateForRestoration warns when older than 7 days`() {
        val eightDaysAgo = now - 8 * 24 * 60 * 60 * 1000L
        val result = JourneyValidator.validateForRestoration(track(startTime = eightDaysAgo, endTime = eightDaysAgo), nowMs = now)
        val warning = result.errors.single { it.code == JourneyErrorCode.STALE_DATA }
        assertEquals(JourneySeverity.WARNING, warning.severity)
        assertTrue(result.isValid) // warning only
    }

    // -- detectGhostJourney -------------------------------------------------

    @Test
    fun `detectGhostJourney valid when nothing is tracking`() {
        val result = JourneyValidator.detectGhostJourney(current(isTracking = false), track())
        assertTrue(result.isValid)
    }

    @Test
    fun `detectGhostJourney case a - tracking but saved track already completed`() {
        val result = JourneyValidator.detectGhostJourney(current(isTracking = true), track(isCompleted = true))
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INCONSISTENT_STATE })
        assertFalse(result.isValid)
    }

    @Test
    fun `detectGhostJourney case b - tracking but saved track already has a transId`() {
        val result = JourneyValidator.detectGhostJourney(current(isTracking = true), track(transId = "txn-1"))
        assertTrue(result.errors.any { it.code == JourneyErrorCode.INCONSISTENT_STATE })
        assertFalse(result.isValid)
    }

    @Test
    fun `detectGhostJourney valid when tracking and saved track state is consistent`() {
        val result = JourneyValidator.detectGhostJourney(current(isTracking = true), track())
        assertTrue(result.isValid)
    }
}
