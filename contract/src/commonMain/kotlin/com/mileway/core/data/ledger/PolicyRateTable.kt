package com.mileway.core.data.ledger

import com.mileway.core.data.model.network.ApprovedVehicle

/** A single vehicle's ₹/km policy rate. */
data class PolicyRate(
    val vehicleKey: String,
    val ratePerKm: Double,
)

/**
 * Rate table + policy knobs consumed by [PolicyRateEngine]. Pure data, no network/DB — build it
 * from live [ApprovedVehicle]s via [fromApprovedVehicles] or hand-roll one for tests.
 */
data class PolicyRateTable(
    val rates: Map<String, Double>,
    val defaultRatePerKm: Double = 0.0,
    val minReimbursement: Double? = null,
    val maxReimbursement: Double? = null,
) {
    fun rateFor(vehicleKey: String): Double = rates[vehicleKey] ?: defaultRatePerKm

    companion object {
        /** Builds a table from a live vehicle list; unknown/null pricing entries are skipped. */
        fun fromApprovedVehicles(
            vehicles: List<ApprovedVehicle>,
            defaultRatePerKm: Double = 0.0,
            minReimbursement: Double? = null,
            maxReimbursement: Double? = null,
        ): PolicyRateTable {
            val rates =
                vehicles
                    .mapNotNull { vehicle ->
                        val key = vehicle.vehicleKey
                        val rate = vehicle.vehiclePricing
                        if (key.isNullOrBlank() || rate == null) null else key to rate
                    }.toMap()
            return PolicyRateTable(rates, defaultRatePerKm, minReimbursement, maxReimbursement)
        }
    }
}
