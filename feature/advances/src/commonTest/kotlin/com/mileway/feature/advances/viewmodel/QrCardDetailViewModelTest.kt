package com.mileway.feature.advances.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockQrCardsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QrCardDetailViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = QrCardDetailViewModel(MockQrCardsRepository(AdvancesRequestStore()))

    @Test
    fun `loading a known card id resolves the card and its recharge history`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(QrCardDetailAction.Load(1L))
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(1L, state.card.dataOrNull?.id)
            assertTrue(state.filteredTransactions.isNotEmpty())
        }

    @Test
    fun `no-voucher filter narrows the recharge log to rows missing a voucher`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(QrCardDetailAction.Load(1L))
            advanceUntilIdle()

            vm.onAction(QrCardDetailAction.SetVoucherFilter(VoucherFilter.NO_VOUCHER))

            val filtered = vm.state.value.filteredTransactions
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { !it.voucherCreated })
        }
}
