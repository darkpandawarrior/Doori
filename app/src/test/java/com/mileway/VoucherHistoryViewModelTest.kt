package com.mileway

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.repository.VoucherStatus
import com.mileway.feature.logging.viewmodel.VOUCHER_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.VoucherHistoryAction
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SP.1 (V17): the voucher-history reducer over the offline fake, All loads everything, a status tab narrows
 * to that status, and the query filters. Proves the F0 HistoryListScaffold MVI contract end-to-end.
 */
class VoucherHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = VoucherHistoryViewModel(VoucherHistoryRepository())

    private fun rows(vm: VoucherHistoryViewModel) =
        (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every voucher`() {
        val vm = viewModel()
        assertEquals(0, vm.state.value.tabIndex)
        assertTrue(rows(vm).isNotEmpty())
        assertEquals(VoucherHistoryRepository().vouchers().size, rows(vm).size)
    }

    @Test
    fun `selecting a status tab narrows to that status`() {
        val vm = viewModel()
        val approvedIndex = VOUCHER_HISTORY_TABS.indexOf(VoucherStatus.APPROVED)
        vm.onAction(VoucherHistoryAction.SelectTab(approvedIndex))
        val rows = rows(vm)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.voucherState == VoucherStatus.APPROVED.label })
    }

    @Test
    fun `query filters by id, tag or office`() {
        val vm = viewModel()
        vm.onAction(VoucherHistoryAction.SetQuery("VCH-1000"))
        val rows = rows(vm)
        assertEquals(1, rows.size)
        assertEquals("VCH-1000", rows.first().id)
    }
}
