package com.mileway

import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.viewmodel.VOUCHER_HISTORY_TABS
import com.mileway.feature.logging.viewmodel.VoucherHistoryAction
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.VoucherRepository
import com.mileway.feature.tracking.viewmodel.CreateVoucherAction
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SP.1/P3.1 (V17/V21): the voucher-history reducer over the shared, Room-backed [VoucherDao] fake — All
 * loads everything, a status tab narrows to that status, and the query filters. Proves the F0
 * HistoryListScaffold MVI contract end-to-end, and (P3.1) that a voucher submitted via
 * `CreateVoucherViewModel` is now visible here, since both bind to the same store.
 */
class VoucherHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(dao: FakeVoucherDao = FakeVoucherDao()) = VoucherHistoryViewModel(VoucherHistoryRepository(dao), VoucherRepository(dao))

    private fun rows(vm: VoucherHistoryViewModel) = (vm.state.value.list as ScreenState.Content).data

    @Test
    fun `All tab loads every seeded voucher`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(0, vm.state.value.tabIndex)
            assertTrue(rows(vm).isNotEmpty())
            assertEquals(8, rows(vm).size)
        }

    @Test
    fun `selecting a status tab narrows to that status`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            val approvedIndex = VOUCHER_HISTORY_TABS.indexOf(VoucherStatus.APPROVED)
            vm.onAction(VoucherHistoryAction.SelectTab(approvedIndex))
            advanceUntilIdle()
            val rows = rows(vm)
            assertTrue(rows.isNotEmpty())
            assertTrue(rows.all { it.voucherState == VoucherStatus.APPROVED.label })
        }

    @Test
    fun `query filters by id, tag or office`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.onAction(VoucherHistoryAction.SetQuery("VCH-1000"))
            advanceUntilIdle()
            val rows = rows(vm)
            assertEquals(1, rows.size)
            assertEquals("VCH-1000", rows.first().id)
        }

    /**
     * P3.1's core acceptance: submitting via Create Voucher must show up in Voucher History —
     * both ViewModels are constructed against the *same* fake [VoucherDao], mirroring how both
     * feature modules now bind to the same Koin-provided Room DAO. History is opened first (as
     * it normally would be, seeding the 8 demo rows), then a voucher is created and submitted,
     * and history's live Flow must pick up the 9th row without a manual refresh.
     */
    @Test
    fun `a voucher submitted via CreateVoucherViewModel appears in VoucherHistoryViewModel`() =
        runTest {
            val sharedDao = FakeVoucherDao()
            val historyVm = viewModel(sharedDao)
            advanceUntilIdle()
            assertEquals(8, rows(historyVm).size)

            val trackDao = FakeSavedTrackDao()
            val trackRepo = SavedTrackRepository(trackDao)
            val voucherRepo = VoucherRepository(sharedDao)
            trackDao.preload(
                com.mileway.core.data.model.db.SavedTrack(
                    routeId = "T1",
                    name = "Journey T1",
                    isCompleted = true,
                    serverUploaded = true,
                    submittedAmount = 250.0,
                    submissionTime = 1L,
                    startLatitude = 0.0,
                    startLongitude = 0.0,
                    endLatitude = 0.0,
                    endLongitude = 0.0,
                    pausedLatitude = 0.0,
                    pausedLongitude = 0.0,
                    startTime = 0L,
                    endTime = 1L,
                    distance = 5_000.0,
                    duration = 60_000L,
                ),
            )
            val createVm = CreateVoucherViewModel(trackRepo, voucherRepo)
            advanceUntilIdle()
            createVm.onAction(CreateVoucherAction.SelectAll)
            createVm.onAction(CreateVoucherAction.SetTitle("Shared Store Voucher"))
            createVm.onAction(CreateVoucherAction.ToggleDeclaration(true))
            createVm.onAction(CreateVoucherAction.Submit)
            advanceUntilIdle()

            val rows = rows(historyVm)
            assertTrue(rows.any { it.amount == 250.0 })
            // 8 seeded demo rows + the 1 just submitted, observed live without a manual Refresh.
            assertEquals(9, rows.size)
        }

    /**
     * P3.6: withdrawing a DRAFT voucher (VCH-1000 is seeded DRAFT — see
     * [VoucherHistoryRepository.demoSeed]) removes it from the live history list without a
     * manual [VoucherHistoryAction.Refresh], since both the writer ([VoucherRepository.withdraw])
     * and the reader ([VoucherHistoryRepository.observeVouchers]) share the same Room-backed DAO.
     */
    @Test
    fun `Withdraw removes a DRAFT voucher from history`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(8, rows(vm).size)
            assertTrue(rows(vm).any { it.id == "VCH-1000" && it.voucherState == VoucherStatus.DRAFT.label })

            vm.onAction(VoucherHistoryAction.Withdraw("VCH-1000"))
            advanceUntilIdle()

            assertEquals(7, rows(vm).size)
            assertTrue(rows(vm).none { it.id == "VCH-1000" })
        }

    /**
     * P3.6's gate: withdraw is a no-op for anything past DRAFT — VCH-1004 is seeded APPROVED
     * (see [VoucherHistoryRepository.demoSeed]) and must survive the call unchanged.
     */
    @Test
    fun `Withdraw is a no-op for a non-DRAFT voucher`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            assertTrue(rows(vm).any { it.id == "VCH-1004" && it.voucherState == VoucherStatus.APPROVED.label })

            vm.onAction(VoucherHistoryAction.Withdraw("VCH-1004"))
            advanceUntilIdle()

            assertEquals(8, rows(vm).size)
            assertTrue(rows(vm).any { it.id == "VCH-1004" })
        }
}
