package com.mileway.feature.profile.analytics

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.PolicyViolationEntity
import com.mileway.core.data.model.db.ReportEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaimAnalyticsTest {
    private val now = 100 * 86_400_000L
    private val paid =
        Report(
            "paid",
            "employee",
            listOf(ExpenseLine("l1", 12_500, "INR", policyFlags = listOf("RECEIPT"), merchant = "Cafe", category = "FOOD")),
            ReportLifecycleState.PAID,
        )
    private val pending =
        Report("pending", "employee", listOf(MileageLine("l2", 725, "USD", distanceKm = 10.0, vehicleKey = "car")), ReportLifecycleState.SUBMITTED)
    private val rows =
        listOf(
            ReportEntity("paid", "employee", "PAID", 4, now - 1_000, now, submittedAtMs = now - 500),
            ReportEntity("pending", "employee", "SUBMITTED", 2, now - 1_000, now, submittedAtMs = now - 500),
        )

    @Test
    fun `currencies stay separate duplicate flags count once and only completed cycles count`() {
        val flags = listOf(PolicyViolationEntity(reportId = "paid", claimLineId = "l1", code = "RECEIPT", message = "Missing receipt", createdAtMs = now))
        val summary = summarizeClaims(listOf(paid, pending), rows, flags, ClaimAnalyticsFilter(), now)
        assertEquals(mapOf("INR" to 12_500L, "USD" to 725L), summary.spendByCurrency)
        assertEquals(mapOf("RECEIPT" to 1), summary.violationsByCode)
        assertEquals(listOf(500L), summary.completedCyclesMillis)
        val paidOnly = summarizeClaims(listOf(paid, pending), rows, flags, ClaimAnalyticsFilter(status = ReportLifecycleState.PAID), now)
        assertEquals(1, paidOnly.reportCount)
        val mileage = summarizeClaims(listOf(paid, pending), rows, flags, ClaimAnalyticsFilter(category = ClaimCategory.MILEAGE), now)
        assertEquals(mapOf("USD" to 725L), mileage.spendByCurrency)
        assertTrue(mileage.completedCyclesMillis.isEmpty())
    }

    @Test
    fun `out of window future and missing submission timestamps are excluded`() {
        val missing = rows.first().copy(submittedAtMs = null)
        assertTrue(summarizeClaims(listOf(paid), listOf(missing), emptyList(), ClaimAnalyticsFilter(), now).completedCyclesMillis.isEmpty())
        listOf(now - 31 * 86_400_000L, now + 1).forEach { created ->
            assertEquals(
                0,
                summarizeClaims(listOf(paid), listOf(rows.first().copy(createdAtMs = created)), emptyList(), ClaimAnalyticsFilter(), now).reportCount,
            )
        }
    }
}
