package com.mileway.feature.tracking.health

import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.core.data.model.db.SavedTrack
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Local inbox candidates, evaluated against one injected clock snapshot. */
class DetectionHealthNudges(
    private val clock: Clock,
) {
    fun evaluate(
        tracks: List<SavedTrack>,
        existing: List<NotificationEntity> = emptyList(),
    ): List<NotificationEntity> {
        val instant = clock.now()
        val now = instant.toEpochMilliseconds()
        val completed = tracks.filter { it.isCompleted }
        val nudges =
            completed
                .filter {
                    it.trackingActivity != "Submitted" && it.notes != "PERSONAL" &&
                        it.endTime > 0 && now - it.endTime > UNCLAIMED_AGE_MS
                }.map { track ->
                    notification(
                        id = "nudge-unclaimed-${track.routeId}",
                        title = "Trip not yet claimed",
                        body = "${track.name} ended more than 3 days ago. Review it before claiming mileage.",
                        now = now,
                        deeplink = "mileway://track/detail/${track.routeId}",
                    )
                }
        val starts = completed.map { it.startTime }.sortedDescending().take(ROLLING_TRIP_COUNT)
        if (starts.size < MIN_COMPLETED_TRIPS) return nudges
        val averageGap = starts.zipWithNext { newer, older -> (newer - older).toDouble() }.average()
        val sinceLatest = now - starts.first()
        if (sinceLatest < MIN_HEALTH_GAP_MS || sinceLatest <= averageGap * HEALTH_GAP_MULTIPLIER) return nudges
        val today = instant.toLocalDateTime(TimeZone.UTC).date
        val hasRecentHealthNudge =
            existing.filter { it.id.startsWith("nudge-health-") }.any { row ->
                val date = runCatching { LocalDate.parse(row.id.removePrefix("nudge-health-")) }.getOrNull()
                row.isUnread || (date != null && date.daysUntil(today) <= HEALTH_COOLDOWN_DAYS)
            }
        if (hasRecentHealthNudge) return nudges
        return nudges +
            notification(
                id = "nudge-health-${instant.toLocalDateTime(TimeZone.UTC).date}",
                title = "Check trip detection",
                body = "No recent trips were recorded. Check tracking permissions if you have been driving.",
                now = now,
                deeplink = "mileway://track",
            )
    }

    private fun notification(
        id: String,
        title: String,
        body: String,
        now: Long,
        deeplink: String,
    ) = NotificationEntity(
        id = id,
        title = title,
        body = body,
        relativeTime = "Just now",
        isUnread = true,
        type = "SYSTEM",
        createdAtMs = now,
        deeplink = deeplink,
    )

    companion object {
        const val ROLLING_TRIP_COUNT = 10
        const val MIN_COMPLETED_TRIPS = 5
        const val HEALTH_GAP_MULTIPLIER = 2
        const val HEALTH_COOLDOWN_DAYS = 7
        const val MIN_HEALTH_GAP_MS = 24L * 60 * 60 * 1000
        const val UNCLAIMED_AGE_MS = 3L * 24 * 60 * 60 * 1000
    }
}
