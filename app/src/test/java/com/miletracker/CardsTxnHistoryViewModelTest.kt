package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.repository.CardTxnStatus
import com.miletracker.feature.logging.repository.CardsTxnHistoryRepository
import com.miletracker.feature.logging.viewmodel.CARDS_TXN_HISTORY_TABS
import com.miletracker.feature.logging.viewmodel.CardsTxnHistoryAction
import com.miletracker.feature.logging.viewmodel.CardsTxnHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** SP.3 (V17): cards-expense-txn history reducer over the offline fake. */
class CardsTxnHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CardsTxnHistoryViewModel(CardsTxnHistoryRepository())

    private fun rows(vm: CardsTxnHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every transaction`() {
        val vm = viewModel()
        assertEquals(CardsTxnHistoryRepository().transactions().size, rows(vm).size)
    }

    @Test
    fun `UNRECONCILED tab narrows to unreconciled only`() {
        val vm = viewModel()
        vm.onAction(CardsTxnHistoryAction.SelectTab(CARDS_TXN_HISTORY_TABS.indexOf(CardTxnStatus.UNRECONCILED)))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == CardTxnStatus.UNRECONCILED.label })
    }
}
