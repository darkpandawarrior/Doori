package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateBusAction
import com.miletracker.feature.travel.viewmodel.CreateBusViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.4 (V17): the Add-Bus reducer, canSubmit gating (both cities + travel date) + the rotating success /
 * approval / violation result paths through the shared FormSubmissionScaffold contract.
 */
class CreateBusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateBusViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until both cities and travel date are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateBusAction.SetFromCity("Pune"))
        vm.onAction(CreateBusAction.SetToCity("Goa"))
        assertFalse(vm.state.value.canSubmit) // date still missing
        vm.onAction(CreateBusAction.SetTravelDate("01-08-2026"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateBusAction.SetFromCity("Pune"))
        vm.onAction(CreateBusAction.SetToCity("Goa"))
        vm.onAction(CreateBusAction.SetTravelDate("01-08-2026"))

        vm.effect.test {
            vm.onAction(CreateBusAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateBusAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateBusAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
