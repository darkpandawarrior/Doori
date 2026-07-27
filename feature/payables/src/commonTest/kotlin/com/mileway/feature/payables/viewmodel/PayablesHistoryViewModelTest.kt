package com.mileway.feature.payables.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.payables.model.PayablesDocStatus
import com.mileway.feature.payables.model.PayablesDocType
import com.mileway.feature.payables.repository.PayablesHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PB.4: covers [PayablesHistoryViewModel] — the type-tab index mapping ([PAYABLES_HISTORY_TABS]), the
 * status-chip filter, the free-text query, and the combinations of all three.
 */
class PayablesHistoryViewModelTest {
    private fun newViewModel(repository: PayablesHistoryRepository = PayablesHistoryRepository()) = PayablesHistoryViewModel(repository)

    @Test
    fun `loads all twelve seeded docs on the All tab by construction`() {
        val vm = newViewModel()

        val docs = vm.state.value.list.dataOrNull
        assertEquals(12, docs?.size)
        assertEquals(0, vm.state.value.tabIndex)
    }

    @Test
    fun `selecting the Invoice tab narrows the list to INVOICE docs only`() {
        val vm = newViewModel()
        val invoiceTabIndex = PAYABLES_HISTORY_TABS.indexOf(PayablesDocType.INVOICE)

        vm.onAction(PayablesHistoryAction.SelectTab(invoiceTabIndex))

        val docs = vm.state.value.list.dataOrNull
        assertEquals(3, docs?.size)
        assertTrue(docs!!.all { it.type == PayablesDocType.INVOICE })
    }

    @Test
    fun `selecting the GIN tab narrows the list to GIN docs only`() {
        val vm = newViewModel()
        val ginTabIndex = PAYABLES_HISTORY_TABS.indexOf(PayablesDocType.GIN)

        vm.onAction(PayablesHistoryAction.SelectTab(ginTabIndex))

        val docs = vm.state.value.list.dataOrNull
        assertEquals(2, docs?.size)
        assertTrue(docs!!.all { it.type == PayablesDocType.GIN })
    }

    @Test
    fun `status filter narrows the All tab to the four pending docs`() {
        val vm = newViewModel()

        vm.onAction(PayablesHistoryAction.SetStatusFilter(PayablesDocStatus.PENDING))

        val docs = vm.state.value.list.dataOrNull
        assertEquals(4, docs?.size)
        assertTrue(docs!!.all { it.status == PayablesDocStatus.PENDING })
    }

    @Test
    fun `query matches across id, title and reference, case-insensitively`() {
        val vm = newViewModel()

        // "Sunrise Traders" / "PO-4821" is shared by one INVOICE row and one GIN row.
        vm.onAction(PayablesHistoryAction.SetQuery("sunrise"))

        val docs = vm.state.value.list.dataOrNull
        assertEquals(2, docs?.size)
        assertTrue(docs!!.all { it.type == PayablesDocType.INVOICE || it.type == PayablesDocType.GIN })
    }

    @Test
    fun `query is trimmed before matching`() {
        val vm = newViewModel()

        // "Apex Logistics" appears in an INVOICE, a GIN and an ASN row — 3 total.
        vm.onAction(PayablesHistoryAction.SetQuery("  apex  "))

        val docs = vm.state.value.list.dataOrNull
        assertEquals(3, docs?.size)
        assertTrue(docs!!.all { it.title.contains("Apex", ignoreCase = true) })
    }

    @Test
    fun `tab, status and query combine and can legitimately yield an empty result`() {
        val vm = newViewModel()
        val ginTabIndex = PAYABLES_HISTORY_TABS.indexOf(PayablesDocType.GIN)
        vm.onAction(PayablesHistoryAction.SelectTab(ginTabIndex))

        // No GIN row is ever APPROVED in the seed spec (only COMPLETED / PENDING).
        vm.onAction(PayablesHistoryAction.SetStatusFilter(PayablesDocStatus.APPROVED))

        assertEquals(emptyList(), vm.state.value.list.dataOrNull)
    }

    @Test
    fun `Refresh reloads the same filtered set without changing tab or status`() {
        val vm = newViewModel()
        val invoiceTabIndex = PAYABLES_HISTORY_TABS.indexOf(PayablesDocType.INVOICE)
        vm.onAction(PayablesHistoryAction.SelectTab(invoiceTabIndex))

        vm.onAction(PayablesHistoryAction.Refresh)

        assertEquals(invoiceTabIndex, vm.state.value.tabIndex)
        assertEquals(3, vm.state.value.list.dataOrNull?.size)
    }
}
