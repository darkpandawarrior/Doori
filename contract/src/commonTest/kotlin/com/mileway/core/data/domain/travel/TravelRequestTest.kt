package com.mileway.core.data.domain.travel

import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.policy.AnnualMileageDistance
import com.mileway.core.data.domain.policy.rates.HmrcMileageRates
import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TravelRequestTest {
    @Test
    fun travelDateSelectsMidyearRateAndFractionalMilesAreNotTruncated() {
        val old = estimateTravel(1.609344, false, "2026-06-30", IrsMileageRates.mirror)
        val next = estimateTravel(1.609344, false, "2026-07-01", IrsMileageRates.mirror)
        assertEquals(73L, old.amountMinor)
        assertEquals(76L, next.amountMinor)
        assertEquals("2026-07-01", next.rateEffectiveFrom)
        assertEquals(38L, estimateTravel(0.804672, false, "2026-07-01", IrsMileageRates.mirror).amountMinor)
    }

    @Test
    fun annualBandCrossingAndPriorYearResetReuseTheMirrorPeriod() {
        val crossing = estimateTravel(3.218688, false, "2026-05-01", HmrcMileageRates.mirror, AnnualMileageDistance("2026-04-06", 9_999))
        assertEquals(80L, crossing.amountMinor)
        val reset = estimateTravel(3.218688, false, "2026-05-01", HmrcMileageRates.mirror, AnnualMileageDistance("2025-04-06", 10_000))
        assertEquals(110L, reset.amountMinor)
        assertEquals(0L, reset.annualDistanceBeforeUnits)
        assertFailsWith<IllegalArgumentException> {
            estimateTravel(1.0, false, "2026-05-01", HmrcMileageRates.mirror, AnnualMileageDistance("2027-04-06", 1))
        }
    }

    @Test
    fun invalidDateDistanceAndOverflowCannotProduceAuthorization() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE).forEach { distance ->
            assertFailsWith<IllegalArgumentException> { estimateTravel(distance, true, "2026-07-01", IrsMileageRates.mirror) }
        }
        listOf("2026-02-30", "2026-7-01", "").forEach { date ->
            assertFailsWith<IllegalArgumentException> { travelDateMillis(date) }
        }
        assertFailsWith<IllegalStateException> { estimateTravel(1.0, true, "2024-12-31", IrsMileageRates.mirror) }
    }

    @Test
    fun serializationKeepsSharedChainAndNoClaimOrPaymentProjection() {
        val submitted = request().transition(ReportLifecycleEvent.SUBMIT)
        val approved = submitted.transition(ReportLifecycleEvent.APPROVE).copy(
            approvalChain = ApprovalChain(listOf(ApprovalStep(0, actedBy = "manager", action = ApprovalAction.APPROVE, actedAtMillis = 1))),
        )
        assertEquals(ReportLifecycleState.APPROVED, approved.state)
        assertEquals(approved, Json.decodeFromString<TravelRequest>(Json.encodeToString(TravelRequest.serializer(), approved)))
        assertEquals(approved.approvalChain, approved.lifecycleReport().approvalChain)
        assertTrue(approved.lifecycleReport().lines.isEmpty())
        assertFailsWith<IllegalArgumentException> { approved.transition(ReportLifecycleEvent.RELEASE_FOR_PAYMENT) }
        assertFailsWith<IllegalArgumentException> { approved.transition(ReportLifecycleEvent.REIMBURSE) }
        assertFailsWith<IllegalArgumentException> { submitted.copy(approvalChain = approved.approvalChain).transition(ReportLifecycleEvent.RECALL) }
    }

    private fun request() = TravelRequest("pretrip-1", "alex", "Client visit", "2026-07-01", estimateTravel(16.09344, false, "2026-07-01", IrsMileageRates.mirror))
}
