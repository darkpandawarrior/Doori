package com.mileway.core.data.domain.policy

/** One per-diem daily rate, in minor currency units, effective from [effectiveFrom] (epoch millis, inclusive). */
data class PerDiemRate(
    val effectiveFrom: Long,
    val dailyRateMinor: Long,
)

/**
 * Dated per-diem rate table, versioned the same way as [PolicyVersion]: [rateFor] resolves the
 * rate in effect at a given instant as the latest [PerDiemRate.effectiveFrom] <= that instant.
 */
data class PerDiemRateTable(
    val rates: List<PerDiemRate>,
) {
    /** The rate in effect at [atMillis]; falls back to the earliest known rate if [atMillis] predates all of them. */
    fun rateFor(atMillis: Long): Long =
        rates.filter { it.effectiveFrom <= atMillis }.maxByOrNull { it.effectiveFrom }?.dailyRateMinor
            ?: rates.minByOrNull { it.effectiveFrom }?.dailyRateMinor
            ?: 0L
}
