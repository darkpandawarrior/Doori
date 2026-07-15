package com.mileway.core.data.ledger

import kotlin.math.roundToLong

/** Why [ReimbursementResult.cappedAmount] differs from the raw [ReimbursementResult.gross]. */
enum class CapReason {
    MIN,
    MAX,
}

/**
 * Result of a single reimbursement calc. [gross] is rate × distance before caps/rounding;
 * [cappedAmount] is the final payable amount after policy caps and rounding to whole currency
 * minor units (paise). [appliedCapReason] is null when no cap fired.
 */
data class ReimbursementResult(
    val ratePerKm: Double,
    val distanceKm: Double,
    val gross: Double,
    val cappedAmount: Long,
    val appliedCapReason: CapReason? = null,
)

/**
 * Pure vehicle→₹/km reimbursement calculator. Consumes a [PolicyRateTable] built from live
 * [com.mileway.core.data.model.network.ApprovedVehicle] pricing or a hand-built one for tests.
 * No network/DB access here — table construction is the caller's job.
 */
class PolicyRateEngine(
    private val table: PolicyRateTable,
) {
    /**
     * Distance <= 0 short-circuits to a zero result at the vehicle's rate (guards bad input from
     * upstream trip data without throwing).
     */
    fun reimbursement(
        vehicleKey: String,
        distanceKm: Double,
    ): ReimbursementResult {
        val rate = table.rateFor(vehicleKey)
        if (distanceKm <= 0.0) {
            return ReimbursementResult(rate, distanceKm, gross = 0.0, cappedAmount = 0L)
        }

        val gross = rate * distanceKm
        var capped = gross
        var reason: CapReason? = null

        table.maxReimbursement?.let { max ->
            if (capped > max) {
                capped = max
                reason = CapReason.MAX
            }
        }
        table.minReimbursement?.let { min ->
            if (capped < min) {
                capped = min
                reason = CapReason.MIN
            }
        }

        // ponytail: whole-paise rounding via roundToLong (matches Formatters.kt's no-String.format
        // idiom); swap for a shared money type if multi-currency minor-unit precision ever matters.
        return ReimbursementResult(rate, distanceKm, gross, capped.roundToLong(), reason)
    }
}
