package com.mileway

import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.feature.logging.report.expenseReportPolicy
import com.mileway.feature.profile.admin.LocalMileageRates
import com.mileway.feature.profile.admin.LocalPolicyRateVersion
import com.mileway.feature.profile.admin.RateTables
import com.mileway.ui.auth.signupPolicySummary
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class PolicySummaryScreenTest {
    private val now = Instant.parse("2026-10-05T00:00:00Z").toEpochMilliseconds()

    @Test
    fun `summary uses existing expense policy and the currency formatter`() {
        val summary = signupPolicySummary(RateTables(LocalMileageRates(), emptyList()), now)
        val policy = expenseReportPolicy().versionFor(now)
        assertTrue(summary.contains("Expense limits (local demo policy)"))
        assertTrue(summary.contains("Expenses above ${formatMinorCurrency(requireNotNull(policy.maxExpenseAmountMinor), policy.currency)} block submission."))
        assertTrue(
            summary.contains(
                "Attach a receipt above ${formatMinorCurrency(requireNotNull(policy.receiptRequiredAboveMinor), policy.currency)}. " +
                    "This check warns before submission.",
            ),
        )
        assertTrue(
            summary.contains("Per-head expenses above ${formatMinorCurrency(requireNotNull(policy.perHeadLimitMinor), policy.currency)} trigger a warning."),
        )
        assertTrue(summary.contains("No employer mileage rate is effective on this device yet."))
    }

    @Test
    fun `summary selects latest effective local rate and excludes future rates`() {
        val rates =
            LocalMileageRates(
                policy =
                    listOf(
                        LocalPolicyRateVersion("2027-01-01", mapOf("future car" to 950L)),
                        LocalPolicyRateVersion("2025-01-01", mapOf("old car" to 700L)),
                        LocalPolicyRateVersion("2026-10-05", mapOf("car" to 850L, "bike" to 325L)),
                    ),
            )
        val summary = signupPolicySummary(RateTables(rates, emptyList()), now)
        assertTrue(summary.contains("Effective from 2026-10-05 (INR)."))
        assertTrue(summary.contains("car: ${formatMinorCurrency(850, "INR")} / km"))
        assertTrue(summary.contains("bike: ${formatMinorCurrency(325, "INR")} / km"))
        assertTrue(summary.none { it.contains("future car") || it.contains("old car") })
        assertTrue(summary.contains("Mileage rates are saved on this device. Claim screens may use different rate sources."))
    }

    @Test
    fun `government mirrors and future employer versions do not become current employer rates`() {
        val rates = LocalMileageRates(policy = listOf(LocalPolicyRateVersion("2027-01-01", mapOf("car" to 900L))))
        val summary = signupPolicySummary(RateTables(rates, emptyList()), now)
        assertTrue(summary.contains("No employer mileage rate is effective on this device yet."))
        assertTrue(summary.none { it.startsWith("Effective from") || it.contains("car:") })
        assertTrue(summary.contains("These are local settings, not a verified employer policy. Government rate references are not employer limits."))
    }
}
