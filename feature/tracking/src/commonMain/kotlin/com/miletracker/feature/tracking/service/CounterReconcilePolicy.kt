package com.miletracker.feature.tracking.service

/**
 * P-A.4: Pure policy for reconciling the DataStore's live point-counter against the
 * authoritative Room database count. The DB is the single source of truth; when they diverge
 * (service kill, reboot, or in-flight crash) the DB wins and the DataStore is corrected before
 * the next accumulation begins.
 *
 * Platform-independent: tested in commonTest, used from both the Android service and the
 * iOS reconciliation hook.
 */
object CounterReconcilePolicy {
    /**
     * Compare [datastoreCount] (from DataStore preferences) against [dbCount] (from
     * `LocationDao.countLocationsByToken`). Returns a [ReconcileResult] indicating whether
     * a write-back is needed and which value to write.
     */
    fun reconcile(
        datastoreCount: Long,
        dbCount: Long,
    ): ReconcileResult =
        ReconcileResult(
            dbCount = dbCount,
            datastoreCount = datastoreCount,
            isDiverged = dbCount != datastoreCount,
        )
}

data class ReconcileResult(
    val dbCount: Long,
    val datastoreCount: Long,
    val isDiverged: Boolean,
) {
    /** Positive = DB has more points (typical after a kill); negative = DB has fewer (abnormal). */
    val delta: Long get() = dbCount - datastoreCount
}
