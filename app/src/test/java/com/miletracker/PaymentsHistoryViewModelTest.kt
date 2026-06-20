package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.payments.model.PaymentStatus
import com.miletracker.feature.payments.repository.PaymentsRepository
import com.miletracker.feature.payments.viewmodel.PAYMENTS_HISTORY_TABS
import com.miletracker.feature.payments.viewmodel.PaymentsHistoryAction
import com.miletracker.feature.payments.viewmodel.PaymentsHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PM (V17): the payments-history reducer — All loads everything, a status tab narrows, and the query filters. */
class PaymentsHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = PaymentsHistoryViewModel(PaymentsRepository())

    private fun rows(vm: PaymentsHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every payment`() {
        val vm = viewModel()
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(PaymentsRepository().payments().size, rows(vm).size)
    }

    @Test
    fun `selecting a status tab narrows to that status`() {
        val vm = viewModel()
        val completedIndex = PAYMENTS_HISTORY_TABS.indexOf(PaymentStatus.COMPLETED)
        vm.onAction(PaymentsHistoryAction.SelectTab(completedIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == PaymentStatus.COMPLETED })
    }

    @Test
    fun `query filters by counterparty or note`() {
        val vm = viewModel()
        vm.onAction(PaymentsHistoryAction.SetQuery("chai"))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.counterparty.contains("chai", ignoreCase = true) })
    }
}
