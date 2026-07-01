package com.mileway

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.SurfaceSnapshotProducer
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals

/**
 * L: the surface snapshot producer is pure (caller-supplied `now`), so today/week bucketing is verified
 * deterministically with a fixed clock in UTC.
 */
class SurfaceSnapshotProducerTest {

    // Fixed "now": 2026-06-21T12:00:00Z.
    private val nowMs = 1_781_956_800_000L
    private val dayMs = 86_400_000L

    private fun track(
        id: String,
        endMs: Long,
        distanceMeters: Double,
    ): SavedTrack =
        SavedTrack(
            routeId = id,
            name = id,
            startLatitude = 0.0, startLongitude = 0.0,
            endLatitude = 0.0, endLongitude = 0.0,
            pausedLatitude = 0.0, pausedLongitude = 0.0,
            startTime = endMs - 600_000, endTime = endMs,
            distance = distanceMeters, duration = 600_000,
            createdAt = endMs, startedAtTimestamp = endMs,
        )

    @Test
    fun `buckets today and this week separately`() {
        val tracks =
            listOf(
                track("today1", nowMs - 3_600_000, 5_000.0), // 1h ago → today
                track("today2", nowMs - 7_200_000, 3_000.0), // 2h ago → today
                track("week1", nowMs - 3 * dayMs, 10_000.0), // 3d ago → this week, not today
                track("old", nowMs - 30 * dayMs, 99_000.0), // 30d ago → neither
            )

        val snap = SurfaceSnapshotProducer.produce(tracks, isTracking = true, nowEpochMs = nowMs, timeZone = TimeZone.UTC)

        assertEquals(2, snap.todayTrips)
        assertEquals(8.0, snap.todayDistanceKm, 0.001)
        assertEquals(3, snap.weekTrips)
        assertEquals(18.0, snap.weekDistanceKm, 0.001)
        assertEquals(true, snap.isTracking)
        assertEquals(nowMs, snap.lastUpdatedEpochMs)
    }

    @Test
    fun `ignores in-progress tracks with no end time`() {
        val tracks = listOf(track("done", nowMs - 3_600_000, 4_000.0).copy(endTime = -1L))
        val snap = SurfaceSnapshotProducer.produce(tracks, isTracking = false, nowEpochMs = nowMs, timeZone = TimeZone.UTC)
        assertEquals(0, snap.todayTrips)
        assertEquals(0.0, snap.todayDistanceKm, 0.001)
    }

    @Test
    fun `L1 - enriches with action-required count, last-trip label, and goal progress`() {
        val tracks =
            listOf(
                track("Morning commute", nowMs - 3_600_000, 5_000.0).copy(serverUploaded = false), // needs action
                track("Site visit", nowMs - 2 * dayMs, 45_000.0).copy(serverUploaded = true), // submitted
            )
        val snap =
            SurfaceSnapshotProducer.produce(
                completedTracks = tracks,
                isTracking = true,
                nowEpochMs = nowMs,
                timeZone = TimeZone.UTC,
                isPaused = true,
                qualityScore = 72,
                weekGoalKm = 100.0,
            )

        assertEquals(1, snap.actionRequiredCount) // only the not-uploaded one
        assertEquals("Morning commute", snap.lastTripLabel) // most recent by endTime
        assertEquals(true, snap.isPaused)
        assertEquals(72, snap.qualityScore)
        // 50 km this week / 100 km goal = 0.5
        assertEquals(0.5, snap.weekGoalProgress.toDouble(), 0.001)
    }
}
