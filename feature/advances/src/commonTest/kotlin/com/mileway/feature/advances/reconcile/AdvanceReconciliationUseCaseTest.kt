package com.mileway.feature.advances.reconcile

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.Report
import com.mileway.feature.advances.data.AdvancesMockData
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockAdvancesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdvanceReconciliationUseCaseTest {
    @Test
    fun smallerLargerAndEqualAdvancesGiveSignedNet() =
        runTest {
            for ((rupees, net) in listOf(5.0 to 500L, 15.0 to -500L, 10.0 to 0L)) {
                val result = useCase(rupees)(report(1_000, "1"), emptyList())
                assertEquals(net, result.netMinor)
                assertEquals(1_000L, result.spendMinor)
                assertTrue(result.excluded.isEmpty())
            }
        }

    @Test
    fun twoAdvancesAreSubtractedOnceAndCopiedClaimAmountsAreIgnored() =
        runTest {
            val result = useCase(2.0, 3.0)(report(1_000, "1", "2"), emptyList())
            assertEquals(500L, result.appliedMinor)
            assertEquals(500L, result.netMinor)
        }

    @Test
    fun reconciledAdvanceOnAnotherReportCannotBeAppliedAgain() =
        runTest {
            val other = Report("other", "employee", listOf(advance("1").copy(reconciled = true)))
            val result = useCase(5.0)(report(1_000, "1"), listOf(other))
            assertEquals(0L, result.appliedMinor)
            assertEquals(1_000L, result.netMinor)
            assertTrue(
                result.excluded
                    .single()
                    .reason
                    .contains("another report"),
            )
        }

    @Test
    fun draftReservationAndDuplicateLinesDoNotApplyTwice() =
        runTest {
            val current = report(1_000, "1", "1")
            val result = useCase(5.0)(current, listOf(current))
            assertEquals(500L, result.appliedMinor)
            assertTrue(
                result.excluded
                    .single()
                    .reason
                    .contains("more than once"),
            )
            val other = current.copy(id = "other", lines = listOf(advance("1")))
            assertEquals(0L, useCase(5.0)(report(1_000, "1"), listOf(other)).appliedMinor)
        }

    @Test
    fun foreignExpenseWithoutPinIsExcludedWithReason() =
        runTest {
            val current = report(1_000, "1").let { it.copy(lines = it.lines + expense(100, "USD")) }
            val result = useCase(5.0)(current, emptyList())
            assertEquals(500L, result.netMinor)
            assertEquals("Claim currency has no usable FX pin to INR", result.excluded.single().reason)
        }

    @Test
    fun foreignExpenseUsesItsSavedPinAndRejectsWrongQuote() =
        runTest {
            val foreign = expense(100, "USD").copy(fxRatePinnedAt = 1, fxRate = FxRate(80.0, "USD", sourceDate = "2026-10-01"))
            val current = report(1_000, "1").let { it.copy(lines = it.lines + foreign) }
            val result = useCase(5.0)(current, emptyList())
            assertEquals(8_500L, result.netMinor)
            assertTrue(result.excluded.isEmpty())
            val bad = current.copy(lines = listOf(expense(1_000), foreign.copy(fxRate = foreign.fxRate!!.copy(quoteCurrency = "EUR")), advance("1")))
            assertEquals(500L, useCase(5.0)(bad, emptyList()).netMinor)
        }

    @Test
    fun foreignAdvanceWithoutPinAndMissingSourceAreExcluded() =
        runTest {
            val cards = MutableStateFlow(listOf(AdvancesMockData.activePettyCards.first().copy(currency = "USD")))
            val repository =
                object : AdvancesRepository by MockAdvancesRepository(AdvancesRequestStore()) {
                    override fun activePettyCards() = cards
                }
            val result = AdvanceReconciliationUseCase(repository)(report(1_000, "1", "unknown"), emptyList())
            assertEquals(0L, result.appliedMinor)
            assertEquals(2, result.excluded.size)
            assertTrue(
                result.excluded
                    .first()
                    .reason
                    .contains("FX pin"),
            )
            assertTrue(
                result.excluded
                    .last()
                    .reason
                    .contains("unavailable"),
            )
        }

    @Test
    fun repositoryIsReadAgainWithoutCachingOrUsingBalanceAndPendingSpend() =
        runTest {
            val card = AdvancesMockData.activePettyCards.first().copy(amount = 5.0, balance = 1.0, txnPendingAmount = 2.0)
            val cards = MutableStateFlow(listOf(card))
            val repository =
                object : AdvancesRepository by MockAdvancesRepository(AdvancesRequestStore()) {
                    override fun activePettyCards() = cards
                }
            val useCase = AdvanceReconciliationUseCase(repository)
            assertEquals(500L, useCase(report(1_000, "1"), emptyList()).appliedMinor)
            cards.value = listOf(card.copy(amount = 7.0))
            assertEquals(700L, useCase(report(1_000, "1"), emptyList()).appliedMinor)
            assertEquals(0L, useCase(report(1_000), emptyList()).appliedMinor)
        }

    @Test
    fun halfEvenUsesDecimalTiesAndHandlesScientificNotation() {
        val cases =
            listOf(
                1.005 to 100L,
                1.015 to 102L,
                2.345 to 234L,
                2.355 to 236L,
                0.005 to 0L,
                0.015 to 2L,
                1.0149 to 101L,
                1.0151 to 102L,
                1e-7 to 0L,
                1e7 to 1_000_000_000L,
            )
        cases.forEach { (rupees, minor) -> assertEquals(minor, rupeesToMinorHalfEven(rupees), "$rupees") }
    }

    @Test
    fun repositoryBoundaryAppliesHalfEvenBeforeReconciliation() =
        runTest {
            val result = useCase(1.005, 1.015)(report(1_000, "1", "2"), emptyList())
            assertEquals(202L, result.appliedMinor)
            assertEquals(798L, result.netMinor)
        }

    @Test
    fun invalidOrOverflowingRepositoryMoneyIsRefused() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.01, Double.MAX_VALUE).forEach { amount ->
            assertFailsWith<IllegalArgumentException> { rupeesToMinorHalfEven(amount) }
        }
    }

    @Test
    fun overflowingTotalsFailInsteadOfWrapping() =
        runTest {
            val current = Report("report", "employee", listOf(expense(Long.MAX_VALUE), expense(1).copy(id = "second")))
            assertFailsWith<IllegalArgumentException> { useCase()(current, emptyList()) }
        }

    private fun useCase(vararg amounts: Double): AdvanceReconciliationUseCase {
        val cards = amounts.mapIndexed { index, amount -> AdvancesMockData.activePettyCards.first().copy(id = index + 1L, amount = amount) }
        val repository =
            object : AdvancesRepository by MockAdvancesRepository(AdvancesRequestStore()) {
                override fun activePettyCards() = MutableStateFlow(cards)
            }
        return AdvanceReconciliationUseCase(repository)
    }

    private fun report(
        spend: Long,
        vararg ids: String,
    ) = Report("report", "employee", listOf(expense(spend)) + ids.map(::advance))

    private fun advance(id: String) = AdvanceLine("line-$id", 999_999, "INR", advanceId = id)

    private fun expense(
        amount: Long,
        currency: String = "INR",
    ) = ExpenseLine("expense-$currency", amount, currency, merchant = "Cafe", category = "FOOD")
}
