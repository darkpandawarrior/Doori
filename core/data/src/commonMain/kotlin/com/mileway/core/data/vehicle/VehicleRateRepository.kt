package com.mileway.core.data.vehicle

import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import kotlinx.coroutines.flow.Flow

/**
 * PLAN_V24 P11.1 — the per-km policy-rate seam. Rates are seeded per persona through the Plugin
 * Registry, not a backend: when [PER_KM_RATES] is on for the active persona (Corporate/Gig policy
 * personas) a vehicle key resolves to its [VehicleCatalog] policy rate; when off (e.g. the consumer
 * persona) there is no rate at all, so no ₹/km chip and no reimbursable amount. This keeps the rate
 * one implementation swap away from a real pricing API — the caller only asks "what's the rate for
 * this key", the source is opaque.
 */
class VehicleRateRepository(private val registry: PluginRegistry) {
    /** Live per-persona toggle: true when policy rates apply for the active account. */
    fun observeRatesEnabled(): Flow<Boolean> = registry.observe(PER_KM_RATES)

    /** True when policy rates apply for the active account right now. */
    suspend fun ratesEnabled(): Boolean = (registry.value(PER_KM_RATES) as? PluginValue.Bool)?.value ?: false

    /** Effective ₹/km for [vehicleKey], or null when policy rates are off for this persona. */
    suspend fun rateFor(vehicleKey: String): Double? = if (ratesEnabled()) VehicleCatalog.rateFor(vehicleKey) else null

    companion object {
        const val PER_KM_RATES = "perKmRatesEnabled"
    }
}
