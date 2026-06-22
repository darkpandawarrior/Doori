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
    /** L.1: true when a live trip is paused (distinct from [isTracking] for the widget's status line). */
    val isPaused: Boolean = false,
    /** Latest live tracking-quality score (0..100); 100 when idle. */
    val qualityScore: Int = 100,
    /** Weekly distance goal (km) the [weekGoalProgress] ring fills toward. */
    val weekGoalKm: Double = DEFAULT_WEEK_GOAL_KM,
    /** Completed trips still needing action (not yet submitted) — drives the widget's action badge. */
    val actionRequiredCount: Int = 0,
    /** Short label for the most recent completed trip (its name), or null if none. */
    val lastTripLabel: String? = null,
) {
    /** This week's progress toward [weekGoalKm], clamped to 0f..1f. */
    val weekGoalProgress: Float
        get() = if (weekGoalKm <= 0.0) 0f else (weekDistanceKm / weekGoalKm).toFloat().coerceIn(0f, 1f)

    companion object {
        const val DEFAULT_WEEK_GOAL_KM = 100.0
    }
}

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
        // L.1: live signals the track list can't supply, plus the configurable weekly goal.
        isPaused: Boolean = false,
        qualityScore: Int = 100,
        weekGoalKm: Double = SurfaceSnapshot.DEFAULT_WEEK_GOAL_KM,
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
        val mostRecent = completed.maxByOrNull { it.endTime }

        return SurfaceSnapshot(
            todayDistanceKm = today.sumOf { it.distance } / 1000.0,
            todayTrips = today.size,
            weekDistanceKm = week.sumOf { it.distance } / 1000.0,
            weekTrips = week.size,
            isTracking = isTracking,
            lastUpdatedEpochMs = nowEpochMs,
            isPaused = isPaused,
            qualityScore = qualityScore,
            weekGoalKm = weekGoalKm,
            // Completed trips not yet uploaded/submitted still need the user's attention.
            actionRequiredCount = completed.count { !it.serverUploaded },
            lastTripLabel = mostRecent?.name?.takeIf { it.isNotBlank() },
        )
    }
}
