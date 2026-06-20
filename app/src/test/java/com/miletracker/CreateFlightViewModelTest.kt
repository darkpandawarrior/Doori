package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateFlightAction
import com.miletracker.feature.travel.viewmodel.CreateFlightViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.3 (V17): the Add-Flight reducer — canSubmit gating (both cities + travel date) + the rotating success /
 * approval / violation result paths through the shared FormSubmissionScaffold contract.
 */
class CreateFlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateFlightViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until both cities and travel date are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateFlightAction.SetFromCity("Pune"))
        vm.onAction(CreateFlightAction.SetToCity("Delhi"))
        assertFalse(vm.state.value.canSubmit) // date still missing
        vm.onAction(CreateFlightAction.SetTravelDate("12-07-2026"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateFlightAction.SetFromCity("Pune"))
        vm.onAction(CreateFlightAction.SetToCity("Delhi"))
        vm.onAction(CreateFlightAction.SetTravelDate("12-07-2026"))

        vm.effect.test {
            vm.onAction(CreateFlightAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateFlightAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateFlightAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
