package com.mileway.feature.tracking.ui.review

import com.mileway.core.ui.components.DistanceLedger
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi

/**
 * Builds the [DistanceLedger] the REVIEW phase's centrepiece renders, from the real fields on
 * [SubmissionFormUi] plus the tracked [distanceKm] the screen was opened with — never from a
 * parallel/made-up figure.
 *
 * ponytail: [SubmissionFormUi] does not (yet) carry the distance pipeline's raw/mock/abnormal/
 * spike buckets that [SavedTrack][com.mileway.core.data.model.db.SavedTrack] already computes
 * (`originalDistance`/`mockDistance`/`abnormalDistance`/`spikeDistance`/`cleanedDistance`) — this
 * file only owns new code under `ui/review`, not the ViewModel those live behind, so raw and
 * cleaned both render as the same tracked [distanceKm] with zero deductions for now. Upgrade path:
 * thread those `SavedTrack` columns onto `SubmissionFormUi` (a ViewModel-layer change) and this
 * function's rawKm/cleanedKm start telling the truer story; the odometer cross-check below is
 * already real.
 */
fun SubmissionFormUi.toDistanceLedger(distanceKm: Double): DistanceLedger =
    DistanceLedger(
        rawKm = distanceKm,
        cleanedKm = distanceKm,
        claimedKm = distanceKm,
        odometerKm = odometerDistanceKm(),
    )

/** The odometer-derived distance for this form, or null until both readings are captured. */
fun SubmissionFormUi.odometerDistanceKm(): Double? {
    val start = simulatedStartOdo
    val end = simulatedEndOdo
    return if (start != null && end != null) (end - start).toDouble() else null
}
