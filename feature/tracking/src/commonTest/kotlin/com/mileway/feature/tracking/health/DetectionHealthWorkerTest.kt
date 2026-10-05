package com.mileway.feature.tracking.health

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.claim.completedTrip
import com.mileway.feature.tracking.viewmodel.FakeSavedTrackDao
import com.mileway.feature.tracking.worker.DetectionHealthWorker
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DetectionHealthWorkerTest {
    private val now = Instant.parse("2026-10-05T12:00:00Z").toEpochMilliseconds()
    private val day = DetectionHealthNudges.MIN_HEALTH_GAP_MS
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(now)
    }

    private fun history(
        count: Int = DetectionHealthNudges.MIN_COMPLETED_TRIPS,
        gap: Long = day,
        sinceLatest: Long = 3 * day,
    ): List<SavedTrack> =
        List(count) { index ->
            completedTrip().copy(
                routeId = "trip-$index",
                startTime = now - sinceLatest - index * gap,
                endTime = now - sinceLatest - index * gap + 1,
                notes = "PERSONAL",
            )
        }

    private fun health(tracks: List<SavedTrack>): List<NotificationEntity> =
        DetectionHealthNudges(clock).evaluate(tracks).filter { it.id.startsWith("nudge-health-") }

    @Test
    fun `five completed trips are required`() {
        assertTrue(health(history(count = 4)).isEmpty())
        assertEquals(1, health(history(count = 5)).size)
        assertTrue(health(history(count = 5).map { it.copy(isCompleted = false) }).isEmpty())
        assertTrue(health(emptyList()).isEmpty())
    }

    @Test
    fun `health gap must be strictly greater than twice the average`() {
        assertTrue(health(history(sinceLatest = 2 * day - 1)).isEmpty())
        assertTrue(health(history(sinceLatest = 2 * day)).isEmpty())
        assertEquals(1, health(history(sinceLatest = 2 * day + 1)).size)
    }

    @Test
    fun `health gap must be at least twenty four hours`() {
        assertTrue(health(history(gap = day / 4, sinceLatest = day - 1)).isEmpty())
        assertEquals(1, health(history(gap = day / 4, sinceLatest = day)).size)
        assertEquals(1, health(history(gap = day / 4, sinceLatest = day + 1)).size)
        assertTrue(health(history(sinceLatest = -day)).isEmpty())
    }

    @Test
    fun `health average uses the latest ten starts regardless of input order`() {
        val recent = history(count = 10)
        val oldOutlier = completedTrip().copy(startTime = now - 365 * day, notes = "PERSONAL")
        assertEquals(health(recent), health((recent + oldOutlier).reversed()))
    }

    @Test
    fun `trip reminders require completion age and a not yet claimed business trip`() {
        val trip = completedTrip().copy(endTime = now - DetectionHealthNudges.UNCLAIMED_AGE_MS - 1)
        val nudge = DetectionHealthNudges(clock).evaluate(listOf(trip)).single()
        assertEquals("nudge-unclaimed-trip-1", nudge.id)
        assertEquals("Trip not yet claimed", nudge.title)
        assertEquals("mileway://track/detail/trip-1", nudge.deeplink)
        assertEquals("SYSTEM", nudge.type)
        assertTrue(nudge.isUnread)
        listOf(
            trip.copy(isCompleted = false),
            trip.copy(trackingActivity = "Submitted"),
            trip.copy(notes = "PERSONAL"),
            trip.copy(endTime = now - DetectionHealthNudges.UNCLAIMED_AGE_MS),
            trip.copy(endTime = now - DetectionHealthNudges.UNCLAIMED_AGE_MS + 1),
            trip.copy(endTime = now + 1),
            trip.copy(endTime = 0),
        ).forEach { assertTrue(DetectionHealthNudges(clock).evaluate(listOf(it)).isEmpty()) }
    }

    @Test
    fun `health reminder has a stable UTC date and tracking deeplink`() {
        val nudge = health(history()).single()
        assertEquals("nudge-health-2026-10-05", nudge.id)
        assertEquals("mileway://track", nudge.deeplink)
        assertEquals("SYSTEM", nudge.type)
        assertEquals(now, nudge.createdAtMs)
    }

    @Test
    fun `repeated worker runs preserve read state for both stable ids`() =
        runTest {
            val trips = history().map { it.copy(notes = "-", endTime = now - 4 * day) }
            val tracks = object : SavedTrackDao by FakeSavedTrackDao() {
                override fun getCompletedTracks() = flowOf(trips)
            }
            val inbox = NudgeNotificationDao()
            val worker = DetectionHealthWorker(tracks, inbox, clock)
            val env = WorkerEnvironment(progressListener = null, isCancelled = { false })
            assertIs<WorkerResult.Success>(worker.doWork(null, env))
            assertEquals(6, inbox.rows.value.size)
            assertEquals(1, inbox.writes)
            inbox.markAllRead()
            val readRows = inbox.rows.value

            assertIs<WorkerResult.Success>(worker.doWork(null, env))

            assertEquals(readRows, inbox.rows.value)
            assertEquals(1, inbox.writes)
            assertFalse(inbox.rows.value.any { it.isUnread })
        }

    @Test
    fun `worker without eligible trips does not write inbox rows`() =
        runTest {
            val inbox = NudgeNotificationDao()
            val worker = DetectionHealthWorker(FakeSavedTrackDao(), inbox, clock)
            assertIs<WorkerResult.Success>(worker.doWork(null, WorkerEnvironment(progressListener = null, isCancelled = { false })))
            assertEquals(0, inbox.writes)
        }
}

private class NudgeNotificationDao : NotificationDao {
    val rows = MutableStateFlow<List<NotificationEntity>>(emptyList())
    var writes = 0

    override fun observeAll() = rows

    override suspend fun count(): Int = rows.value.size

    override suspend fun countNonNudge(): Int = rows.value.count { !it.id.startsWith("nudge-") }

    override suspend fun upsertAll(entities: List<NotificationEntity>) {
        writes++
        rows.value = (rows.value + entities).associateBy { it.id }.values.toList()
    }

    override suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    ) {
        rows.value = rows.value.map { if (it.id == id) it.copy(isUnread = isUnread) else it }
    }

    override suspend fun markAllRead() {
        rows.value = rows.value.map { it.copy(isUnread = false) }
    }
}
