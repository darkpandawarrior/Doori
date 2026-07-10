package com.mileway

import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.viewmodel.VoucherDetailsAction
import com.mileway.feature.logging.viewmodel.VoucherDetailsViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P27.E.12: the voucher-details drill-down reducer over the shared, Room-backed [VoucherDao] fake
 * — loads the exact row by [VoucherEntity.voucherNumber], and reports "not found" as
 * [ScreenState.Empty] rather than crashing or erroring for a stale/withdrawn id.
 */
class VoucherDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun seeded(): FakeVoucherDao =
        FakeVoucherDao().apply {
            kotlinx.coroutines.runBlocking {
                insert(
                    VoucherEntity(
                        voucherNumber = "VCH-9001",
                        title = "Client Visit",
                        category = VoucherCategory.MILEAGE,
                        totalAmount = 1_250.0,
                        notes = "Pune-Mumbai round trip",
                        expenseRouteIdsJson = VoucherEntity.encodeExpenseRouteIds(listOf("EXP-1", "EXP-2")),
                        status = "Pending",
                        createdAtMs = 1_700_000_000_000L,
                    ),
                )
            }
        }

    @Test
    fun `Load resolves an existing voucher by number`() = runTest {
        val vm = VoucherDetailsViewModel(seeded())
        vm.onAction(VoucherDetailsAction.Load("VCH-9001"))
        advanceUntilIdle()

        val state = vm.state.value.voucher
        assertTrue(state is ScreenState.Content)
        assertEquals("Client Visit", state.data.title)
        assertEquals(1_250.0, state.data.totalAmount)
    }

    @Test
    fun `Load resolves to Empty for an unknown voucher number`() = runTest {
        val vm = VoucherDetailsViewModel(seeded())
        vm.onAction(VoucherDetailsAction.Load("VCH-DOES-NOT-EXIST"))
        advanceUntilIdle()

        assertEquals(ScreenState.Empty, vm.state.value.voucher)
    }
}
