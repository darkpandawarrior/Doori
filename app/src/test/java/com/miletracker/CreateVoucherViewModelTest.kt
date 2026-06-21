package com.miletracker

import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.repository.VoucherRepository
import com.miletracker.feature.tracking.viewmodel.CreateVoucherAction
import com.miletracker.feature.tracking.viewmodel.CreateVoucherViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreateVoucherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeSavedTrackDao()
    private val trackRepo = SavedTrackRepository(dao)
    private val voucherRepo = VoucherRepository()

    private fun viewModel() = CreateVoucherViewModel(trackRepo, voucherRepo)

    /**
     * Creates a completed + server-uploaded [SavedTrack] whose display data will have
     * [isSubmitted = true] and [reimbursableAmount = amount], the two filters
     * [CreateVoucherViewModel.loadExpenses] applies.
     */
    private fun makeSubmittedTrack(routeId: String, amount: Double = 50.0) = SavedTrack(
        routeId = routeId,
        name = "Journey $routeId",
        isCompleted = true,
        serverUploaded = true,
        submittedAmount = amount,
        submissionTime = 1L,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 0L, endTime = 1L,
        distance = 5_000.0, duration = 60_000L,
    )

    @Test
    fun `init loads submitted expenses with non-zero amount`() = runTest {
        dao.preload(makeSubmittedTrack("T1", 100.0))
        dao.preload(makeSubmittedTrack("T2", 0.0))   // zero amount: excluded
        dao.preload(makeSubmittedTrack("T3", 200.0))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.expenses.size)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `init generates a default title`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.title.startsWith("Voucher:"))
    }

    @Test
    fun `ToggleSelection adds and removes tokens`() = runTest {
        dao.preload(makeSubmittedTrack("T1"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(CreateVoucherAction.ToggleSelection("T1"))
        assertTrue("T1" in vm.state.value.selectedTokens)

        vm.onAction(CreateVoucherAction.ToggleSelection("T1"))
        assertFalse("T1" in vm.state.value.selectedTokens)
    }

    @Test
    fun `SelectAll selects all expense tokens`() = runTest {
        dao.preload(makeSubmittedTrack("T1"))
        dao.preload(makeSubmittedTrack("T2"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(CreateVoucherAction.SelectAll)
        assertEquals(setOf("T1", "T2"), vm.state.value.selectedTokens)
    }

    @Test
    fun `DeselectAll clears selection`() = runTest {
        dao.preload(makeSubmittedTrack("T1"))
        dao.preload(makeSubmittedTrack("T2"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(CreateVoucherAction.SelectAll)
        vm.onAction(CreateVoucherAction.DeselectAll)
        assertTrue(vm.state.value.selectedTokens.isEmpty())
    }

    @Test
    fun `totalAmount sums reimbursable amounts of selected tokens`() = runTest {
        dao.preload(makeSubmittedTrack("T1", 100.0))
        dao.preload(makeSubmittedTrack("T2", 200.0))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(CreateVoucherAction.ToggleSelection("T1"))
        assertEquals(100.0, vm.totalAmount, 1e-9)

        vm.onAction(CreateVoucherAction.ToggleSelection("T2"))
        assertEquals(300.0, vm.totalAmount, 1e-9)
    }

    @Test
    fun `SetTitle and SetCategory update form fields`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateVoucherAction.SetTitle("Q4 2026"))
        vm.onAction(CreateVoucherAction.SetCategory("Fuel"))
        assertEquals("Q4 2026", vm.state.value.title)
        assertEquals("Fuel", vm.state.value.category)
    }

    @Test
    fun `SetNotes updates notes field`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateVoucherAction.SetNotes("Approved by manager"))
        assertEquals("Approved by manager", vm.state.value.notes)
    }

    @Test
    fun `GoToStep navigates between wizard steps`() = runTest {
        val vm = viewModel()
        assertEquals(0, vm.state.value.step)
        vm.onAction(CreateVoucherAction.GoToStep(1))
        assertEquals(1, vm.state.value.step)
        vm.onAction(CreateVoucherAction.GoToStep(2))
        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun `Submit saves voucher and advances to step 3`() = runTest {
        dao.preload(makeSubmittedTrack("T1", 150.0))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(CreateVoucherAction.SelectAll)
        vm.onAction(CreateVoucherAction.SetTitle("March Voucher"))
        vm.onAction(CreateVoucherAction.Submit)
        advanceUntilIdle()

        assertEquals(3, vm.state.value.step)
        assertNotNull(vm.state.value.submittedVoucherNumber)
        assertFalse(vm.state.value.isSubmitting)
        assertEquals(1, voucherRepo.vouchers.size)
        assertEquals("March Voucher", voucherRepo.vouchers.first().title)
        assertEquals(150.0, voucherRepo.vouchers.first().totalAmount, 1e-9)
    }
}
