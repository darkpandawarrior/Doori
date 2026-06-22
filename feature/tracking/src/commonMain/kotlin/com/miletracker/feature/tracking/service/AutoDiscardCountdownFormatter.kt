package com.miletracker.feature.tracking.service

/**
 * P-D.3: formats the time remaining before an inactive session is auto-discarded.
 * Used by the notification, Live Activity, and hero-card countdown chip.
 */
object AutoDiscardCountdownFormatter {
    /**
     * Returns a human-readable remaining-time string:
     * - "X m Y s" while > 60 s remaining
     * - "Y s" while ≤ 60 s remaining
     * - "Discarding…" when remainingMs ≤ 0
     */
    fun format(remainingMs: Long): String {
        if (remainingMs <= 0L) return "Discarding…"
        val totalSec = remainingMs / 1_000L
        val minutes = totalSec / 60L
        val seconds = totalSec % 60L
        return if (minutes > 0L) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    /**
     * Returns the notification text line to append when an auto-discard countdown is active.
     * e.g. "Trip will be discarded in 4m 32s"
     */
    fun notificationText(remainingMs: Long): String = "Trip will be discarded in ${format(remainingMs)}"
}
