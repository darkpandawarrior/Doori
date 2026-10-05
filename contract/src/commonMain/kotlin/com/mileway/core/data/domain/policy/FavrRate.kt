package com.mileway.core.data.domain.policy

import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import kotlinx.serialization.Serializable

/** Published maximum standard automobile cost in USD minor units, including trucks and vans. */
@Serializable
data class FavrCostLimit(
    val effectiveFrom: String,
    val maxStandardAutomobileCostMinor: Long,
    val sourceTitle: String,
    val sourceUrl: String,
) {
    init {
        rateDateMillis(effectiveFrom)
        require(maxStandardAutomobileCostMinor > 0) { "FAVR cap must be positive" }
        require(sourceTitle.isNotBlank() && sourceUrl.startsWith("https://")) { "FAVR cap citation is required" }
    }
}

/**
 * Employer-set USD FAVR inputs: a fixed payment per period plus a variable payment per whole mile.
 * The variable rate is thousandths of a minor unit. Period count is explicit so fixed payments are
 * never implicitly repeated per trip. This calculates payments and validates the published automobile
 * cost cap; it does not certify the other eligibility requirements of Rev. Proc. 2019-46.
 */
@Serializable
data class FavrRate(
    val fixedPaymentMinorPerPeriod: Long,
    val variableRateThousandthsMinor: Long,
    val standardAutomobileCostMinor: Long,
) {
    init {
        require(fixedPaymentMinorPerPeriod >= 0 && variableRateThousandthsMinor >= 0) { "FAVR payments must be nonnegative" }
        require(standardAutomobileCostMinor > 0) { "Standard automobile cost must be positive" }
    }

    /** Fixed periods plus variable distance, rounded half-up once. The cap is resolved at submission. */
    fun amountMinor(
        distanceMiles: Long,
        fixedPeriods: Long,
        submittedAtMillis: Long,
        irsRates: MileageRateMirror = IrsMileageRates.mirror,
    ): Long {
        require(irsRates.currency == "USD") { "FAVR automobile cost cap must be in USD" }
        val cap = irsRates.favrLimitFor(submittedAtMillis)
        require(standardAutomobileCostMinor <= cap.maxStandardAutomobileCostMinor) { "Standard automobile cost exceeds the IRS FAVR cap" }
        val fixed = checkedRateProduct(fixedPaymentMinorPerPeriod, fixedPeriods)
        val variable = roundRateMinor(checkedRateProduct(variableRateThousandthsMinor, distanceMiles))
        return checkedRateSum(fixed, variable)
    }
}
