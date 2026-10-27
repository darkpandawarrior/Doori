package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.LocationData
import com.mileway.feature.tracking.repository.LocationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wave-2 batching: buffers points in memory and writes them to Room via
 * [LocationRepository.insertBatch] instead of one `insert` per GPS fix — cuts IO/battery.
 *
 * Flushes when the buffer reaches [MAX_BATCH_SIZE] points, or when [MAX_BATCH_AGE_MS] has
 * elapsed since the last flush, whichever comes first. [flush] forces an out-of-band flush and
 * MUST be called on every lifecycle transition that could precede process death (stop, pause,
 * service teardown) — see LocationTrackingService call sites — so a clean stop/pause never loses
 * buffered points. A crash can still lose the current in-flight window; that's the accepted
 * tradeoff (parity spec §2.2).
 *
 * [now] is injected so tests can drive the 30s window with a fake clock.
 */
class LocationBatcher(
    private val repository: LocationRepository,
    private val now: () -> Long,
) {
    companion object {
        // named consts per spec — 10pt/30s batching cadence.
        const val MAX_BATCH_SIZE = 10
        const val MAX_BATCH_AGE_MS = 30_000L
    }

    private val mutex = Mutex()
    private val buffer = mutableListOf<LocationData>()
    private var lastFlushAt: Long = now()

    /** Buffers [point]; flushes when the batch is full or the age window has elapsed. */
    suspend fun add(point: LocationData) {
        val shouldFlush =
            mutex.withLock {
                buffer.add(point)
                buffer.size >= MAX_BATCH_SIZE || now() - lastFlushAt >= MAX_BATCH_AGE_MS
            }
        if (shouldFlush) flush()
    }

    /** Forces a flush of whatever is currently buffered (no-op if empty). Call on stop/pause/teardown. */
    suspend fun flush() {
        val toWrite =
            mutex.withLock {
                if (buffer.isEmpty()) return
                val copy = buffer.toList()
                buffer.clear()
                lastFlushAt = now()
                copy
            }
        repository.insertBatch(toWrite)
    }
}
