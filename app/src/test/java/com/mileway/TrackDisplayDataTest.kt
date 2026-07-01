package com.mileway

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.model.display.toDisplayData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackDisplayDataTest {

    private fun makeTrack(
        distance: Double = 8700.0,
        startTime: Long = 1_000_000L,
        endTime: Long = 1_100_000L,
        serverUploaded: Boolean = true
    ) = SavedTrack(
        routeId = "test-id",
        name = "Test Track",
        startLatitude = 18.5, startLongitude = 73.8,
        endLatitude = 18.6, endLongitude = 73.9,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = startTime, endTime = endTime,
        distance = distance, duration = endTime - startTime,
        serverUploaded = serverUploaded,
        submittedAmount = distance / 1000.0 * 10.0,
        submissionTime = if (serverUploaded) endTime + 10_000L else 0L
    )

    @Test
    fun `toDisplayData maps distance in km correctly`() {
        val track = makeTrack(distance = 8700.0).toDisplayData()
        assertEquals(8.7, track.distanceKm, 0.001)
        assertEquals("8.70 km", track.getFormattedDistance())
    }

    @Test
    fun `toDisplayData maps submission state`() {
        val submitted = makeTrack(serverUploaded = true).toDisplayData()
        val draft = makeTrack(serverUploaded = false).toDisplayData()
        assertTrue(submitted.isSubmitted)
        assertFalse(draft.isSubmitted)
    }

    @Test
    fun `getTrackingState returns COMPLETED for finished track`() {
        val track = makeTrack().toDisplayData()
        assertEquals(TrackingState.COMPLETED, track.getTrackingState())
    }

    @Test
    fun `getDurationMs calculates correctly`() {
        val track = makeTrack(startTime = 1_000_000L, endTime = 4_600_000L).toDisplayData()
        assertEquals(3_600_000L, track.getDurationMs())
    }

    @Test
    fun `getFormattedDuration shows hours and minutes for long tracks`() {
        val track = makeTrack(startTime = 1_000_000L, endTime = 4_660_000L).toDisplayData()
        assertEquals("1h 1m", track.getFormattedDuration())
    }

    @Test
    fun `getFormattedDuration shows only minutes for short tracks`() {
        val track = makeTrack(startTime = 1_000_000L, endTime = 1_600_000L).toDisplayData()
        assertEquals("10m", track.getFormattedDuration())
    }
}
