package com.mileway.feature.tracking.service

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
        val diffSec = (nowMs - timestampMs).coerceAtLeast(0L) / 1000L
        return when {
            diffSec < 60L -> "just now"
            diffSec < 3_600L -> "${diffSec / 60L} min ago"
            diffSec < 86_400L -> "${diffSec / 3_600L} hr ago"
            else -> "${diffSec / 86_400L} d ago"
        }
    }
}
