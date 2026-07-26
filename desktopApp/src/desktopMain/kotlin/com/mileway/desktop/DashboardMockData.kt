package com.mileway.desktop

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.model.display.SurfaceSnapshotProducer
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.model.display.toDisplayData
import kotlinx.datetime.TimeZone

/**
 * D.2: hardcoded mock trips for the desktop dashboard — Option b is a thin app with no
 * `feature:tracking`/Room-repository dependency, so (mirroring `:sharedWatch`'s `WatchDomainFacade`
 * pattern of building directly on `core:data` types) the trip list here is a fixed `SavedTrack` list
 * rather than a live query. Real persistence is a later phase per CLAUDE.md "The backend".
 */
private const val DAY_MS = 86_400_000L

fun mockCompletedTracks(nowEpochMs: Long): List<SavedTrack> =
    listOf(
        mockTrack(routeId = "d1", name = "Client site visit", distanceM = 18_400.0, endTime = nowEpochMs - 2 * 3_600_000L),
        mockTrack(routeId = "d2", name = "Airport pickup", distanceM = 42_100.0, endTime = nowEpochMs - DAY_MS),
        mockTrack(routeId = "d3", name = "Warehouse run", distanceM = 9_800.0, endTime = nowEpochMs - 3 * DAY_MS),
    )

private fun mockTrack(
    routeId: String,
    name: String,
    distanceM: Double,
    endTime: Long,
) = SavedTrack(
    routeId = routeId,
    name = name,
    isCompleted = true,
    startLatitude = 0.0,
    startLongitude = 0.0,
    endLatitude = 0.0,
    endLongitude = 0.0,
    pausedLatitude = 0.0,
    pausedLongitude = 0.0,
    startTime = endTime - 3_600_000L,
    endTime = endTime,
    distance = distanceM,
    duration = 3_600_000L,
    createdAt = endTime,
)

/**
 * The dashboard's [SurfaceSnapshot], folded from [mockCompletedTracks] via the shared producer.
 *
 * [timeZone] is injectable purely so tests can pin it. The producer buckets "today" by local calendar
 * day, so with the default system zone the same fixed [nowEpochMs] yields a different `todayTrips`
 * depending on where the machine is — which is how `DashboardMockDataTest` passed in IST and failed on
 * a UTC CI runner the first time this task was wired into the gate.
 */
fun mockSnapshot(
    nowEpochMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): SurfaceSnapshot =
    SurfaceSnapshotProducer.produce(
        completedTracks = mockCompletedTracks(nowEpochMs),
        isTracking = false,
        nowEpochMs = nowEpochMs,
        timeZone = timeZone,
    )

/** Trip-list rows for the dashboard, newest first. */
fun mockTripRows(nowEpochMs: Long): List<TrackDisplayData> =
    mockCompletedTracks(nowEpochMs)
        .map { it.toDisplayData() }
        .sortedByDescending { it.endTime }
