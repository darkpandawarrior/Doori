package com.mileway.feature.tracking.claim

import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.ledger.ReimbursementResult
import com.mileway.core.data.model.db.SavedTrack

/** Maps persisted tracked metres to a policy-priced mileage line, using the trip end's policy version. */
class TripToClaimMapper(
    private val policyEngine: PolicyEngine,
) {
    /** Pricing shared with the success screen. Policy tables here carry currency minor units. */
    fun reimbursement(
        vehicleKey: String,
        distanceKm: Double,
        atMillis: Long,
    ): ReimbursementResult = PolicyRateEngine(policyEngine.versionFor(atMillis).rateTable).reimbursement(vehicleKey, distanceKm)

    /** Incomplete, discarded, simulated, unowned and invalid-distance trips cannot fund a report. */
    fun map(track: SavedTrack): MileageLine? {
        if (!track.isEligibleForMileageClaim()) return null
        val distanceKm = track.distance / METRES_PER_KM
        val line =
            MileageLine(
                id = "mileage_line_${track.routeId}",
                amountMinor = reimbursement(track.selectedVehicleType, distanceKm, track.endTime).cappedAmount,
                currency = "INR",
                sourceTripId = track.routeId,
                distanceKm = distanceKm,
                vehicleKey = track.selectedVehicleType,
            )
        return line.copy(policyFlags = policyEngine.evaluate(listOf(line), track.endTime).getValue(line.id).map { it.code })
    }

    private companion object {
        const val METRES_PER_KM = 1_000.0
    }
}

internal fun SavedTrack.isEligibleForMileageClaim(): Boolean =
    isCompleted &&
        !isDiscarded &&
        !wasMockOn &&
        !wasMockLocationUsed &&
        (startedByEmployeeCode.isNotBlank() || !startedByAccountId.isNullOrBlank()) &&
        distance.isFinite() &&
        distance >= 0.0 &&
        endTime > 0L
