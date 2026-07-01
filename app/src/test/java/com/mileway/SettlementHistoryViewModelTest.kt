package com.mileway

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.repository.SettlementHistoryRepository
import com.mileway.feature.logging.repository.SettlementStatus
import com.mileway.feature.logging.viewmodel.SETTLEMENT_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.SettlementHistoryAction
import com.mileway.feature.logging.viewmodel.SettlementHistoryViewModel
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
