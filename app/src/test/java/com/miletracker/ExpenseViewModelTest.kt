package com.miletracker

import app.cash.turbine.test
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.model.ExpenseCategory
import com.miletracker.feature.logging.model.ExpenseRecord
import com.miletracker.feature.logging.model.ExpenseStatus
import com.miletracker.feature.logging.repository.ExpenseRepository
import com.miletracker.feature.logging.viewmodel.ExpenseAction
import com.miletracker.feature.logging.viewmodel.ExpenseEffect
import com.miletracker.feature.logging.viewmodel.ExpenseFilter
import com.miletracker.feature.logging.viewmodel.ExpenseListData
import com.miletracker.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * H: behavioural coverage for [ExpenseViewModel] — the expense list + multi-step submission reducer.
 * The repository is a concrete in-memory mock (no deps).
 */
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = ExpenseViewModel(ExpenseRepository())

    private fun listData(vm: ExpenseViewModel): ExpenseListData {
        val s = vm.state.value.listState
        assertTrue(s is ScreenState.Content<ExpenseListData>)
        return (s as ScreenState.Content<ExpenseListData>).data
    }

    @Test
    fun `init loads all expenses under the ALL filter`() {
        val data = listData(viewModel())
        assertEquals(ExpenseFilter.ALL, data.activeFilter)
        assertEquals(ExpenseRepository().getAll().size, data.records.size)
    }

    @Test
    fun `SetFilter narrows the list to the chosen status`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetFilter(ExpenseFilter.DRAFTS))
        val data = listData(vm)
        assertEquals(ExpenseFilter.DRAFTS, data.activeFilter)
        assertTrue(data.records.isNotEmpty())
        assertTrue(data.records.all { it.status == ExpenseStatus.DRAFT })
    }

    @Test
    fun `SelectCategory advances the form to step two`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.entries.first()))
        assertEquals(2, vm.state.value.form.step)
        assertEquals(ExpenseCategory.entries.first(), vm.state.value.form.category)
    }

    @Test
    fun `SubmitExpense records the amount and navigates to success`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            val effect = awaitItem()
            assertTrue(effect is ExpenseEffect.NavigateToSuccess)
            assertEquals((effect as ExpenseEffect.NavigateToSuccess).id, vm.state.value.lastSubmittedId)
        }
        assertEquals(249.50, vm.state.value.lastSubmittedAmount)
    }

    @Test
    fun `OpenDetail resolves a known id and falls back to Empty for an unknown one`() {
        val vm = viewModel()
        val known: ExpenseRecord = ExpenseRepository().getAll().first()
        vm.onAction(ExpenseAction.OpenDetail(known.id))
        assertTrue(vm.state.value.detailState is ScreenState.Content<ExpenseRecord>)

        vm.onAction(ExpenseAction.OpenDetail("does-not-exist"))
        assertEquals(ScreenState.Empty, vm.state.value.detailState)
    }

    @Test
    fun `ResetForm clears the form and last submission`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetMerchant("X"))
        vm.onAction(ExpenseAction.ResetForm)
        assertEquals("", vm.state.value.form.merchantName)
        assertEquals("", vm.state.value.lastSubmittedId)
    }
}
