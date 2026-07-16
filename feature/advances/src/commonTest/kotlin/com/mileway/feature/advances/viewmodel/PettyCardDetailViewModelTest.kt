package com.mileway.feature.advances.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockAdvancesRepository
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
class PettyCardDetailViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = PettyCardDetailViewModel(MockAdvancesRepository(AdvancesRequestStore()))

    @Test
    fun `loading a known card id resolves the card and its transactions`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(PettyCardDetailAction.Load(1L))
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(1L, state.card.dataOrNull?.id)
            assertTrue(state.filteredTransactions.isNotEmpty())
        }

    @Test
    fun `has-voucher filter narrows the transaction list to voucherCreated rows only`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(PettyCardDetailAction.Load(1L))
            advanceUntilIdle()
            val unfiltered = vm.state.value.filteredTransactions

            vm.onAction(PettyCardDetailAction.SetVoucherFilter(VoucherFilter.HAS_VOUCHER))
            val filtered = vm.state.value.filteredTransactions

            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.size < unfiltered.size)
            assertTrue(filtered.all { it.voucherCreated })
        }

    @Test
    fun `a search query narrows transactions to titles containing it`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(PettyCardDetailAction.Load(1L))
            advanceUntilIdle()
            val title = vm.state.value.filteredTransactions.first().title

            vm.onAction(PettyCardDetailAction.SetQuery(title))

            assertTrue(vm.state.value.filteredTransactions.all { it.title.contains(title, ignoreCase = true) })
            assertTrue(vm.state.value.filteredTransactions.isNotEmpty())
        }
}
