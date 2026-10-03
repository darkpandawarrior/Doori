package com.mileway.core.data.claim

/**
 * L2: the one-shot gate [LegacyMileageBackfillWorker] checks before running — commonMain interface
 * so the worker stays platform-agnostic and unit-testable; [com.mileway.core.data.claim.DataStoreBackfillMarker]
 * (androidMain) is the DataStore-backed implementation, keyed by a schema-version marker so a later
 * schema bump can force a re-run by changing the key rather than needing new migration logic.
 */
interface BackfillMarker {
    suspend fun isDone(): Boolean

    suspend fun markDone()
}
