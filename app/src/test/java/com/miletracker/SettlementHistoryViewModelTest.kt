package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.repository.SettlementHistoryRepository
import com.miletracker.feature.logging.repository.SettlementStatus
import com.miletracker.feature.logging.viewmodel.SETTLEMENT_HISTORY_TABS
import com.miletracker.feature.logging.viewmodel.SettlementHistoryAction
import com.miletracker.feature.logging.viewmodel.SettlementHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** SP.2 (V17): settlement-history reducer over the offline fake. */
class SettlementHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = SettlementHistoryViewModel(SettlementHistoryRepository())

    private fun rows(vm: SettlementHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every settlement`() {
        val vm = viewModel()
        assertEquals(SettlementHistoryRepository().settlements().size, rows(vm).size)
    }

    @Test
    fun `SETTLED tab narrows to settled only`() {
        val vm = viewModel()
        vm.onAction(SettlementHistoryAction.SelectTab(SETTLEMENT_HISTORY_TABS.indexOf(SettlementStatus.SETTLED)))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == SettlementStatus.SETTLED.label })
    }
}
