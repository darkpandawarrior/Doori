package com.miletracker.core.data.model.display

import com.miletracker.core.data.model.db.SavedTrack
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * A small, serializable summary of tracking activity for off-screen *surfaces* (L/N), an Android Glance
 * home-screen widget and (later) an iOS WidgetKit timeline. Deliberately platform-neutral and tiny so it can
 * be written to a shared store (DataStore / App Group) and re-rendered cheaply by either widget host.
 */
data class SurfaceSnapshot(
    val todayDistanceKm: Double = 0.0,
    val todayTrips: Int = 0,
    val weekDistanceKm: Double = 0.0,
    val weekTrips: Int = 0,
    val isTracking: Boolean = false,
    val lastUpdatedEpochMs: Long = 0L,
)

/**
 * Pure producer for a [SurfaceSnapshot] (L), folds the completed-track list into today's and this-week's
 * distance + trip counts relative to a caller-supplied `nowEpochMs` (no `Clock.System` read, so it is fully
 * deterministic and JVM-unit-testable). Shared by every widget host: the platform renderers (Glance /
 * WidgetKit) just lay out the result.
 */
object SurfaceSnapshotProducer {
    private const val DAY_MS = 86_400_000L

    fun produce(
        completedTracks: List<SavedTrack>,
        isTracking: Boolean,
        nowEpochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): SurfaceSnapshot {
        val todayStart =
            Instant.fromEpochMilliseconds(nowEpochMs)
                .toLocalDateTime(timeZone)
                .date
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds()
        val weekStart = nowEpochMs - 7 * DAY_MS
        val completed = completedTracks.filter { it.endTime > 0L }

        val today = completed.filter { it.endTime >= todayStart }
        val week = completed.filter { it.endTime >= weekStart }

        return SurfaceSnapshot(
            todayDistanceKm = today.sumOf { it.distance } / 1000.0,
            todayTrips = today.size,
            weekDistanceKm = week.sumOf { it.distance } / 1000.0,
            weekTrips = week.size,
            isTracking = isTracking,
            lastUpdatedEpochMs = nowEpochMs,
        )
    }
}
