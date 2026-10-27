package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.EventType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wave-2 SmartEventLogger (parity §2.8): per-[EventType] throttle policy for high-frequency
 * hardware events (repeated MOCK_LOCATION/ABNORMAL_LOCATION per GPS fix) so noisy event types
 * don't hit the DB on every occurrence. Critical one-shot lifecycle events are never throttled —
 * [CRITICAL_TYPES] is an allowlist, not the throttled set, so any [EventType] not given an explicit
 * interval in [INTERVALS_MS] also passes through unthrottled by default (additive: this only
 * suppresses types we've explicitly named as noisy).
 *
 * [now] is injected so tests can drive the throttle window with a fake clock.
 */
class EventThrottler(
    private val now: () -> Long,
) {
    companion object {
        // named per-type minimum intervals for known-noisy event types (parity §2.8).
        // Types absent from this map are never throttled.
        const val MOCK_LOCATION_INTERVAL_MS = 30_000L
        const val ABNORMAL_LOCATION_INTERVAL_MS = 30_000L

        private val INTERVALS_MS =
            mapOf(
                EventType.MOCK_LOCATION to MOCK_LOCATION_INTERVAL_MS,
                EventType.ABNORMAL_LOCATION to ABNORMAL_LOCATION_INTERVAL_MS,
            )

        // Critical one-shot lifecycle events: always allowed even back-to-back.
        private val CRITICAL_TYPES =
            setOf(
                EventType.TRACKING_STARTED,
                EventType.TRACKING_STOPPED,
                EventType.TRACKING_PAUSED,
                EventType.TRACKING_RESUMED,
            )
    }

    private val mutex = Mutex()
    private val lastSeenAt = mutableMapOf<EventType, Long>()

    /** Returns true if [eventType] should be logged right now, false if it should be suppressed. */
    suspend fun allow(eventType: EventType): Boolean {
        if (eventType in CRITICAL_TYPES) return true
        val interval = INTERVALS_MS[eventType] ?: return true

        return mutex.withLock {
            val nowMs = now()
            val last = lastSeenAt[eventType]
            if (last != null && nowMs - last < interval) {
                false
            } else {
                lastSeenAt[eventType] = nowMs
                true
            }
        }
    }
}
