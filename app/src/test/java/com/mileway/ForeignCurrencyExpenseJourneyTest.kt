package com.mileway

import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.network.fx.FxRatePinner
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ForeignCurrencyExpenseJourneyTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitAndEditKeepThePinnedRateAndForeignAmount() = runTest {
        var calls = 0
        val repo = ExpenseRepository()
        val pinner = FxRatePinner(reference = { _, _, _ -> calls++; FxRate(90.0, "USD", sourceDate = "2026-09-25") }, nowMillis = { 123 })
        val vm = ExpenseViewModel(repo, fxPinner = pinner)
        fill(vm)
        vm.onAction(ExpenseAction.SubmitExpense)
        advanceUntilIdle()
        val id = vm.state.value.lastSubmittedId
        assertTrue(id.isNotEmpty())
        val saved = repo.getById(id)!!
        assertEquals("USD", saved.currencyCode)
        assertEquals(1000L, saved.amountMinor)
        assertEquals(90.0, saved.fxRate?.rate)
        assertEquals(123L, saved.fxRatePinnedAt)
        vm.onAction(ExpenseAction.OpenEdit(id))
        vm.onAction(ExpenseAction.SubmitExpense)
        advanceUntilIdle()
        assertEquals(saved.fxRate, repo.getById(id)?.fxRate)
        assertEquals(1, calls)
    }

    @Test
    fun offlineManualRateIsApproximateAndMissingRateSkipsPolicyWithoutCrashing() = runTest {
        val repo = ExpenseRepository()
        val vm = ExpenseViewModel(repo)
        fill(vm)
        vm.onAction(ExpenseAction.SetManualFxRate("80"))
        vm.onAction(ExpenseAction.SubmitExpense)
        advanceUntilIdle()
        val saved = repo.getById(vm.state.value.lastSubmittedId)!!
        assertEquals(FxRateSource.MANUAL_APPROXIMATE, saved.fxRate?.source)
        assertNull(saved.fxRate?.sourceDate)
        assertTrue(saved.fxRatePinnedAt != null)
        vm.onAction(ExpenseAction.ResetForm)
        fill(vm)
        vm.onAction(ExpenseAction.SetMerchant("No Rate Cafe"))
        vm.onAction(ExpenseAction.SubmitExpense)
        advanceUntilIdle()
        assertTrue(vm.state.value.fxMessage.orEmpty().contains("checks are skipped"))
        vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
        advanceUntilIdle()
        assertNull(repo.getById(vm.state.value.lastSubmittedId)?.fxRate)
        assertTrue(vm.state.value.lastSubmissionViolations.any { it.id == "FX_POLICY_SKIPPED" })
    }

    private fun fill(vm: ExpenseViewModel) {
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Foreign Cafe"))
        vm.onAction(ExpenseAction.SetCurrency("USD"))
        vm.onAction(ExpenseAction.SetAmount("10.00"))
    }
}
