package com.mileway.core.data.domain.policy

import kotlinx.serialization.Serializable

/** A dated mileage schedule with the publication identifying that version. */
@Serializable
data class MileageRateVersion(
    val effectiveFrom: String,
    val schedule: AnnualDistanceStepDown,
    val authority: String,
) {
    init {
        rateDateMillis(effectiveFrom)
        require(authority.isNotBlank()) { "Rate authority is required" }
    }
}

/** Cited offline data. FAVR plan payment amounts are employer inputs and never mirror data. */
@Serializable
data class MileageRateMirror(
    val sourceTitle: String,
    val sourceUrl: String,
    val retrievedOn: String,
    val currency: String,
    val annualPeriod: MileageAnnualPeriod,
    val versions: List<MileageRateVersion>,
    val favrCostLimits: List<FavrCostLimit> = emptyList(),
) {
    init {
        require(sourceTitle.isNotBlank() && sourceUrl.startsWith("https://")) { "Rate citation is required" }
        rateDateMillis(retrievedOn)
        require(currency.isNotBlank() && versions.isNotEmpty()) { "Currency and rate versions are required" }
        require(versions.map { it.effectiveFrom }.distinct().size == versions.size) { "Duplicate mileage effective date" }
        require(versions.map { it.schedule.distanceUnit }.distinct().size == 1) { "Distance unit must stay consistent across versions" }
        require(favrCostLimits.map { it.effectiveFrom }.distinct().size == favrCostLimits.size) { "Duplicate FAVR cap effective date" }
    }

    /** Latest effectiveFrom <= submission time. A date before the first published version is rejected. */
    fun versionFor(submittedAtMillis: Long): MileageRateVersion =
        versions.filter { rateDateMillis(it.effectiveFrom) <= submittedAtMillis }.maxByOrNull { it.effectiveFrom }
            ?: error("No mileage rate in effect at submission time")

    /**
     * Prices a trip using the submission version and cumulative business distance for that annual period.
     * A prior period's total resets to zero. Future totals and misaligned period starts are rejected.
     * The returned minor units use [currency]; all distances use the resolved schedule's own unit.
     */
    fun amountMinor(
        distanceUnits: Long,
        submittedAtMillis: Long,
        annualDistance: AnnualMileageDistance? = null,
    ): Long {
        val periodStart = annualPeriod.startFor(submittedAtMillis)
        annualDistance?.let {
            require(annualPeriod.startFor(rateDateMillis(it.periodStart)) == it.periodStart) { "Annual distance period is misaligned" }
            require(it.periodStart <= periodStart) { "Annual distance belongs to a future period" }
        }
        val before = annualDistance?.takeIf { it.periodStart == periodStart }?.distanceUnits ?: 0L
        return versionFor(submittedAtMillis).schedule.amountMinor(distanceUnits, before)
    }

    /** Resolves a cap only within its publication year. No unverified cap carries into another year. */
    fun favrLimitFor(submittedAtMillis: Long): FavrCostLimit {
        val year = MileageAnnualPeriod.CALENDAR_YEAR.startFor(submittedAtMillis).substringBefore('-')
        return favrCostLimits
            .filter { it.effectiveFrom.substringBefore('-') == year && rateDateMillis(it.effectiveFrom) <= submittedAtMillis }
            .maxByOrNull { it.effectiveFrom }
            ?: error("No published FAVR automobile cost cap for submission year")
    }
}
