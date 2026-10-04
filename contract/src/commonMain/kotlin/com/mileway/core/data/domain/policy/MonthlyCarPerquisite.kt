package com.mileway.core.data.domain.policy

import kotlinx.serialization.Serializable

private const val SMALL_ENGINE_CC = 1_600

/** Monthly motor-car perquisite values in INR minor units, distinct from mileage reimbursement. */
@Serializable
data class MonthlyCarPerquisiteVersion(
    val effectiveFrom: String,
    val upTo1600CcMinor: Long,
    val above1600CcMinor: Long,
    val chauffeurMinor: Long,
) {
    init {
        rateDateMillis(effectiveFrom)
        require(upTo1600CcMinor >= 0 && above1600CcMinor >= 0 && chauffeurMinor >= 0) { "Perquisite values must be nonnegative" }
    }

    /** One month's mixed-use value when the employer owns or hires the car and meets expenses. */
    fun amountMinor(
        engineCapacityCc: Int,
        chauffeurProvided: Boolean,
    ): Long {
        require(engineCapacityCc > 0) { "Engine capacity must be positive" }
        val car = if (engineCapacityCc <= SMALL_ENGINE_CC) upTo1600CcMinor else above1600CcMinor
        return checkedRateSum(car, if (chauffeurProvided) chauffeurMinor else 0)
    }
}

/** Cited monthly data with an explicit verification limitation. It has no per-distance pricing API. */
@Serializable
data class MonthlyCarPerquisiteMirror(
    val kind: String,
    val currency: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val retrievedOn: String,
    val verification: String,
    val versions: List<MonthlyCarPerquisiteVersion>,
) {
    init {
        require(kind == "MONTHLY" && currency == "INR") { "Motor-car perquisites are monthly INR values" }
        require(sourceTitle.isNotBlank() && sourceUrl.startsWith("https://") && verification.isNotBlank()) { "Perquisite citation is required" }
        rateDateMillis(retrievedOn)
        require(versions.isNotEmpty() && versions.map { it.effectiveFrom }.distinct().size == versions.size) { "Unique monthly versions are required" }
    }

    /** Latest effectiveFrom <= submission time, rejecting dates before the first mirrored version. */
    fun versionFor(submittedAtMillis: Long): MonthlyCarPerquisiteVersion =
        versions.filter { rateDateMillis(it.effectiveFrom) <= submittedAtMillis }.maxByOrNull { it.effectiveFrom }
            ?: error("No monthly car perquisite in effect at submission time")
}
