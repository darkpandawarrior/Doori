package com.mileway.feature.tracking.claim

import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.PolicyVersion
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

/** Reuses the success screen's approved vehicle rates and offline fallback, expressed in paise. */
class MileageClaimPolicyProvider(
    private val vehiclePricingRepository: VehiclePricingRepository,
) {
    suspend fun mapper(): TripToClaimMapper {
        val vehicles =
            runCatching { vehiclePricingRepository.getVehicles() }.getOrElse { failure ->
                if (failure is CancellationException || failure !is Exception) throw failure
                Napier.w("Vehicle rates unavailable; using offline mileage fallback", failure, tag = "MileageClaimPolicy")
                emptyList()
            }
        val rupeeTable = PolicyRateTable.fromApprovedVehicles(vehicles, defaultRatePerKm = DEFAULT_RATE_PER_KM)
        val minorTable =
            rupeeTable.copy(
                rates = rupeeTable.rates.mapValues { (_, rate) -> rate * MINOR_UNIT_SCALE },
                defaultRatePerKm = rupeeTable.defaultRatePerKm * MINOR_UNIT_SCALE,
            )
        // ponytail: the offline dataset has one undated rate table. Add dated versions when
        // the policy backend supplies them; TripToClaimMapper already selects by trip end time.
        return TripToClaimMapper(PolicyEngine(listOf(PolicyVersion(effectiveFrom = 0L, rateTable = minorTable))))
    }

    companion object {
        const val DEFAULT_RATE_PER_KM = 8.0
        const val MINOR_UNIT_SCALE = 100.0
    }
}
