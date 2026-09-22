package com.mileway

import com.mileway.core.data.watch.SnapshotCache
import com.mileway.core.data.watch.WatchSyncPayload

/**
 * In-memory fake for [SnapshotCache] (P6.1) — backed by a plain (non-thread-confined) `var`, not a
 * real DataStore/`NSUserDefaults`, mirroring [FakeActiveAccountSource]'s shape so JVM tests can
 * exercise "write in one process, read from another" without a platform-backed `Context`. A shared
 * instance handed to two independent readers/writers stands in for the cross-process contract —
 * the real platform actuals ([com.mileway.core.data.watch.SnapshotCacheStore]) are what actually
 * cross the process boundary; this fake only proves the write-then-read contract shape.
 */
class FakeSnapshotCache(
    seed: WatchSyncPayload? = null,
) : SnapshotCache {
    private var stored: WatchSyncPayload? = seed

    override suspend fun write(payload: WatchSyncPayload) {
        stored = payload
    }

    override suspend fun read(): WatchSyncPayload? = stored
}
