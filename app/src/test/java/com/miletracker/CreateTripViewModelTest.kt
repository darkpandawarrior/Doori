package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateTripAction
import com.miletracker.feature.travel.viewmodel.CreateTripViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.2 (V17): the Create-Trip reducer — canSubmit gating (purpose + both cities) + the rotating success /
 * approval / violation result paths through the shared FormSubmissionScaffold contract.
 */
class CreateTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateTripViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until purpose and both cities are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateTripAction.SetPurpose("Client visit"))
        vm.onAction(CreateTripAction.SetFromCity("Pune"))
        assertFalse(vm.state.value.canSubmit) // destination still missing
        vm.onAction(CreateTripAction.SetToCity("Mumbai"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateTripAction.SetPurpose("Client visit"))
        vm.onAction(CreateTripAction.SetFromCity("Pune"))
        vm.onAction(CreateTripAction.SetToCity("Mumbai"))

        vm.effect.test {
            vm.onAction(CreateTripAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateTripAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateTripAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
