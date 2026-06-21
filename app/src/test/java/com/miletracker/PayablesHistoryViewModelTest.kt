package com.miletracker

import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.payables.model.PayablesDocStatus
import com.miletracker.feature.payables.model.PayablesDocType
import com.miletracker.feature.payables.repository.PayablesHistoryRepository
import com.miletracker.feature.payables.viewmodel.PAYABLES_HISTORY_TABS
import com.miletracker.feature.payables.viewmodel.PayablesHistoryAction
import com.miletracker.feature.payables.viewmodel.PayablesHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PB.4 (V17): the unified payables-history reducer over the offline fake, All loads every document family, a
 * type tab narrows to that family, a status filter chip narrows further, and the query filters. Proves the F0
 * HistoryListScaffold MVI contract for the Invoice/PR/GIN/Park/ASN surface.
 */
class PayablesHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = PayablesHistoryViewModel(PayablesHistoryRepository())

    private fun rows(vm: PayablesHistoryViewModel) =
        (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every document family`() {
        val vm = viewModel()
        assertEquals(0, vm.state.value.tabIndex)
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(PayablesHistoryRepository().documents().size, rows(vm).size)
        assertTrue(rows(vm).map { it.type }.toSet().containsAll(PayablesDocType.entries.toSet()))
    }

    @Test
    fun `selecting a type tab narrows to that family`() {
        val vm = viewModel()
        val ginIndex = PAYABLES_HISTORY_TABS.indexOf(PayablesDocType.GIN)
        vm.onAction(PayablesHistoryAction.SelectTab(ginIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.type == PayablesDocType.GIN })
    }

    @Test
    fun `status filter narrows to that status`() {
        val vm = viewModel()
        vm.onAction(PayablesHistoryAction.SetStatusFilter(PayablesDocStatus.PENDING))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.status == PayablesDocStatus.PENDING })
    }

    @Test
    fun `query filters by id, title or reference`() {
        val vm = viewModel()
        vm.onAction(PayablesHistoryAction.SetQuery("Sunrise"))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.title.contains("Sunrise", ignoreCase = true) })
    }
}
