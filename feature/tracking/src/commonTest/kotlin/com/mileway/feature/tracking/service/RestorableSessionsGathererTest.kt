package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.SavedTrack
import kotlin.test.Test
import kotlin.test.assertEquals

/** Wave-4 §2.1: [RestorableSessionsGatherer] gathers N local sessions with a per-session validation status. */
class RestorableSessionsGathererTest {
    private fun track(
        routeId: String,
        startedByAccountId: String? = "acc-1",
        distance: Double = 500.0,
        duration: Long = 60_000L,
        startedAt: Long = 0L,
        isCompleted: Boolean = false,
        isDiscarded: Boolean = false,
        isDraft: Boolean = false,
    ) = SavedTrack(
        routeId = routeId,
        name = "trip",
        isCompleted = isCompleted,
        isDiscarded = isDiscarded,
        isDraft = isDraft,
        startedByAccountId = startedByAccountId,
        startedAtTimestamp = startedAt,
        startLatitude = 0.0,
        startLongitude = 0.0,
        endLatitude = 0.0,
        endLongitude = 0.0,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 0L,
        endTime = 0L,
        distance = distance,
        duration = duration,
    )

    @Test
    fun `gathers every non-completed non-discarded track as a restorable session`() {
        val tracks =
            listOf(
                track("r1"),
                track("r2"),
                track("r3", isCompleted = true),
                track("r4", isDiscarded = true),
            )

        val result = RestorableSessionsGatherer.gather(tracks, activeAccountId = "acc-1")

        assertEquals(setOf("r1", "r2"), result.map { it.routeId }.toSet())
    }

    @Test
    fun `a track owned by a different persona validates as OWNER_MISMATCH`() {
        val tracks = listOf(track("r1", startedByAccountId = "acc-2"))

        val result = RestorableSessionsGatherer.gather(tracks, activeAccountId = "acc-1")

        assertEquals(SessionValidationStatus.OWNER_MISMATCH, result.single().status)
    }

    @Test
    fun `a track with no recorded distance or duration validates as EMPTY`() {
        val tracks = listOf(track("r1", distance = 0.0, duration = 0L))

        val result = RestorableSessionsGatherer.gather(tracks, activeAccountId = "acc-1")

        assertEquals(SessionValidationStatus.EMPTY, result.single().status)
    }

    @Test
    fun `a track owned by the active persona with recorded data validates as VALID`() {
        val tracks = listOf(track("r1", startedByAccountId = "acc-1", distance = 500.0, duration = 60_000L))

        val result = RestorableSessionsGatherer.gather(tracks, activeAccountId = "acc-1")

        assertEquals(SessionValidationStatus.VALID, result.single().status)
    }

    @Test
    fun `results are ordered newest-first by startedAtMs`() {
        val tracks = listOf(track("old", startedAt = 100L), track("new", startedAt = 200L))

        val result = RestorableSessionsGatherer.gather(tracks, activeAccountId = "acc-1")

        assertEquals(listOf("new", "old"), result.map { it.routeId })
    }

    @Test
    fun `two or more restorable sessions is the multi-restore trigger condition`() {
        val singleSession = listOf(track("r1"))
        val multipleSessions = listOf(track("r1"), track("r2"))

        assertEquals(1, RestorableSessionsGatherer.gather(singleSession, activeAccountId = "acc-1").size)
        assertEquals(2, RestorableSessionsGatherer.gather(multipleSessions, activeAccountId = "acc-1").size)
    }
}
