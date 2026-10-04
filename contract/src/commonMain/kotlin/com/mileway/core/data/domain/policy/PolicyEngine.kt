package com.mileway.core.data.domain.policy

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.amountInCurrencyMinor
import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.ledger.PolicyRateTable

/** Whether a [PolicyViolation] blocks submission outright or only warns the submitter. */
enum class PolicySeverity {
    HARD_BLOCK,
    SOFT_WARN,
}

/** A single, high-confidence policy check result attached to one [ClaimLine]. */
data class PolicyViolation(
    val code: String,
    val severity: PolicySeverity,
    val message: String,
)

/**
 * One dated version of the policy ruleset, effective from [effectiveFrom] (epoch millis,
 * inclusive) until a later [PolicyVersion]'s [effectiveFrom] supersedes it. [rateTable] backs
 * mileage evaluation via the existing [PolicyRateEngine]; [maxExpenseAmountMinor] is the hard
 * ceiling on a single expense line; [receiptRequiredAboveMinor] is the soft-warn threshold above
 * which a receipt is expected but not yet attached (attachment itself is out of scope here).
 */
data class PolicyVersion(
    val effectiveFrom: Long,
    val rateTable: PolicyRateTable,
    val maxExpenseAmountMinor: Long? = null,
    val receiptRequiredAboveMinor: Long? = null,
    val perHeadLimitMinor: Long? = null,
    val currency: String = "INR",
)

/**
 * Generalizes [PolicyRateEngine]/[PolicyRateTable] (kept unchanged — other lanes depend on those
 * symbols) into a policy check over every [ClaimLine] subtype, versioned by effective date so a
 * rate or limit change never rewrites the evaluation of an already-submitted claim. [versions]
 * need not be pre-sorted.
 */
class PolicyEngine(
    private val versions: List<PolicyVersion>,
) {
    /** The version in effect at [atMillis]: the latest [PolicyVersion.effectiveFrom] <= [atMillis]. */
    fun versionFor(atMillis: Long): PolicyVersion =
        versions.filter { it.effectiveFrom <= atMillis }.maxByOrNull { it.effectiveFrom }
            ?: versions.minByOrNull { it.effectiveFrom }
            ?: error("PolicyEngine requires at least one PolicyVersion")

    /** Evaluates every line against the [PolicyVersion] in effect at [submittedAtMillis], keyed by line id. */
    fun evaluate(
        lines: List<ClaimLine>,
        submittedAtMillis: Long,
    ): Map<String, List<PolicyViolation>> {
        val version = versionFor(submittedAtMillis)
        return lines.associate { it.id to evaluateLine(it, version) }
    }

    private fun evaluateLine(
        line: ClaimLine,
        version: PolicyVersion,
    ): List<PolicyViolation> =
        when (line) {
            is ExpenseLine -> evaluateExpense(line, version)
            is MileageLine -> evaluateMileage(line, version)
            // ponytail: per-diem/advance checks are narrow by design (no high-confidence rule
            // for them yet); wire PerDiemRateTable in here once claim capture needs it.
            is PerDiemLine -> emptyList()
            is AdvanceLine -> emptyList()
        }

    private fun evaluateExpense(
        line: ExpenseLine,
        version: PolicyVersion,
    ): List<PolicyViolation> {
        val amount =
            line.amountInCurrencyMinor(version.currency)
                ?: return listOf(
                    PolicyViolation(
                        "FX_POLICY_SKIPPED",
                        PolicySeverity.SOFT_WARN,
                        "Amount checks skipped: no pinned ${line.currency} to ${version.currency} rate. Enter a manual rate (approximate).",
                    ),
                )
        val violations = mutableListOf<PolicyViolation>()
        if (line.currency != version.currency && line.fxRate?.source == FxRateSource.MANUAL_APPROXIMATE) {
            violations += PolicyViolation("FX_APPROXIMATE", PolicySeverity.SOFT_WARN, "Amount checks use an approximate manual FX rate")
        }
        version.maxExpenseAmountMinor?.let { max ->
            if (amount > max) {
                violations +=
                    PolicyViolation(
                        code = "EXPENSE_OVER_MAX",
                        severity = PolicySeverity.HARD_BLOCK,
                        message = "Amount $amount exceeds policy max $max",
                    )
            }
        }
        version.receiptRequiredAboveMinor?.let { threshold ->
            if (amount > threshold) {
                violations +=
                    PolicyViolation(
                        code = "RECEIPT_RECOMMENDED",
                        severity = PolicySeverity.SOFT_WARN,
                        message = "Amount $amount exceeds $threshold; attach a receipt",
                    )
            }
        }
        perHeadViolation(amount, line.attendees.size, version)?.let { violations += it }
        return violations
    }

    /** Evaluates the live attendee divisor against the policy in effect at submission time. */
    fun perHeadViolation(
        amountMinor: Long,
        attendeeCount: Int,
        submittedAtMillis: Long,
    ): PolicyViolation? = perHeadViolation(amountMinor, attendeeCount, versionFor(submittedAtMillis))

    private fun perHeadViolation(
        amountMinor: Long,
        attendeeCount: Int,
        version: PolicyVersion,
    ): PolicyViolation? {
        val limit = version.perHeadLimitMinor ?: return null
        if (attendeeCount <= 0 || amountMinor < 0 || limit < 0) return null
        // Compare the exact rational per-head amount, without overflow or rounding a cent away.
        val quotient = amountMinor / attendeeCount
        val overLimit = quotient > limit || (quotient == limit && amountMinor % attendeeCount != 0L)
        return if (overLimit) {
            PolicyViolation("EXPENSE_PER_HEAD_OVER_LIMIT", PolicySeverity.SOFT_WARN, "Per-head expense exceeds the policy limit")
        } else {
            null
        }
    }

    private fun evaluateMileage(
        line: MileageLine,
        version: PolicyVersion,
    ): List<PolicyViolation> {
        val result = PolicyRateEngine(version.rateTable).reimbursement(line.vehicleKey, line.distanceKm)
        val violations = mutableListOf<PolicyViolation>()
        if (line.amountMinor > result.cappedAmount) {
            violations +=
                PolicyViolation(
                    code = "MILEAGE_OVER_POLICY_RATE",
                    severity = PolicySeverity.HARD_BLOCK,
                    message = "Claimed ${line.amountMinor} exceeds policy-computed ${result.cappedAmount}",
                )
        } else if (result.appliedCapReason != null) {
            violations +=
                PolicyViolation(
                    code = "MILEAGE_RATE_CAPPED",
                    severity = PolicySeverity.SOFT_WARN,
                    message = "Policy rate capped by ${result.appliedCapReason}",
                )
        }
        return violations
    }
}
