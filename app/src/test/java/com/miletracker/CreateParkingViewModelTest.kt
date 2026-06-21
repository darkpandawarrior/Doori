package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.payables.repository.ParkMode
import com.miletracker.feature.payables.repository.ParkingRepository
import com.miletracker.feature.payables.viewmodel.CreateParkingAction
import com.miletracker.feature.payables.viewmodel.CreateParkingEffect
import com.miletracker.feature.payables.viewmodel.CreateParkingViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PB.3 (V17): the Create Park In/Out reducer — mode toggle, canSubmit gating (vehicle + gate), and the rotating
 * success / approval / violation result paths through the FormSubmissionScaffold contract.
 */
class CreateParkingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateParkingViewModel(ParkingRepository())

    @Test
    fun `mode defaults to Park In and can switch to Park Out`() {
        val vm = viewModel()
        assertEquals(ParkMode.IN, vm.state.value.mode)
        vm.onAction(CreateParkingAction.SetMode(ParkMode.OUT))
        assertEquals(ParkMode.OUT, vm.state.value.mode)
    }

    @Test
    fun `canSubmit is false until vehicle number and gate are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
        assertFalse(vm.state.value.canSubmit) // gate still missing
        vm.onAction(CreateParkingAction.SetGate("Dock 3"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
        vm.onAction(CreateParkingAction.SetGate("Dock 3"))

        vm.effect.test {
            vm.onAction(CreateParkingAction.Submit)
            assertTrue(awaitItem() is CreateParkingEffect.Success)
            vm.onAction(CreateParkingAction.Submit)
            assertTrue(awaitItem() is CreateParkingEffect.NeedsApproval)
            vm.onAction(CreateParkingAction.Submit)
            assertTrue(awaitItem() is CreateParkingEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
