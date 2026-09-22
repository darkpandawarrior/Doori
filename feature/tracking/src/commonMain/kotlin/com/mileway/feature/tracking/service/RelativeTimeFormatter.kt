package com.mileway.feature.tracking.service

import com.mileway.core.data.util.MillisPerSecond

private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 60L * SecondsPerMinute
private const val SecondsPerDay = 24L * SecondsPerHour

/**
 * Wave-4 §2.3: formats a past timestamp relative to now, for the sync-status chip
 * ("Last synced 3 min ago") and the multi-session restore list.
 */
object RelativeTimeFormatter {
    /**
     * Returns a human-readable relative-time string:
     * - "just now" under 60s
     * - "X min ago" under an hour
     * - "X hr ago" under a day
     * - "X d ago" otherwise
     */
    fun format(
        timestampMs: Long,
        nowMs: Long,
    ): String {
        val diffSec = (nowMs - timestampMs).coerceAtLeast(0L) / MillisPerSecond
        return when {
            diffSec < SecondsPerMinute -> "just now"
            diffSec < SecondsPerHour -> "${diffSec / SecondsPerMinute} min ago"
            diffSec < SecondsPerDay -> "${diffSec / SecondsPerHour} hr ago"
            else -> "${diffSec / SecondsPerDay} d ago"
        }
    }
}
