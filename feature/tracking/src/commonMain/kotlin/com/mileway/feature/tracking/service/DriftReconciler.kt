package com.mileway.feature.tracking.service

/**
 * Wave-2: every [RECONCILE_INTERVAL_MS] (120s), compares the in-memory point count against the
 * DB truth (`LocationDao.countLocationsByToken`, wired at the call site) and reports whether a
 * correction is needed. Batched writes ([LocationBatcher]) mean the in-memory counter can run
 * briefly ahead of what's actually landed in Room — this is the periodic safety net, distinct
 * from [CounterReconcilePolicy]'s one-shot resume/pause reconciliation.
 *
 * Pure decision logic: the caller supplies the current time and both counts, and applies the
 * correction (there's no DB/session dependency here — reuses [CounterReconcilePolicy] for the
 * comparison itself).
 */
class DriftReconciler(
    private val now: () -> Long,
) {
    companion object {
        // named per spec — 120s drift reconciliation cadence.
        const val RECONCILE_INTERVAL_MS = 120_000L
    }

    private var lastReconcileAt: Long = now()

    /**
     * Returns a [ReconcileResult] if [RECONCILE_INTERVAL_MS] has elapsed since the last check
     * (always advancing the internal clock so callers can poll every fix cheaply), or null if
     * it's not yet due.
     */
    fun maybeReconcile(
        inMemoryCount: Long,
        dbCount: Long,
    ): ReconcileResult? {
        val nowMs = now()
        if (nowMs - lastReconcileAt < RECONCILE_INTERVAL_MS) return null
        lastReconcileAt = nowMs
        return CounterReconcilePolicy.reconcile(datastoreCount = inMemoryCount, dbCount = dbCount)
    }
}
