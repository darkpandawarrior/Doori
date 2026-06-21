package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateHotelAction
import com.miletracker.feature.travel.viewmodel.CreateHotelViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.5 (V17): the Add-Hotel reducer, canSubmit gating (city + both stay dates + guests) + the rotating
 * success / approval / violation result paths through the shared FormSubmissionScaffold contract.
 */
class CreateHotelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateHotelViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until city and both stay dates are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateHotelAction.SetCity("Mumbai"))
        vm.onAction(CreateHotelAction.SetCheckInDate("12-07-2026"))
        assertFalse(vm.state.value.canSubmit) // check-out still missing
        vm.onAction(CreateHotelAction.SetCheckOutDate("15-07-2026"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateHotelAction.SetCity("Mumbai"))
        vm.onAction(CreateHotelAction.SetCheckInDate("12-07-2026"))
        vm.onAction(CreateHotelAction.SetCheckOutDate("15-07-2026"))

        vm.effect.test {
            vm.onAction(CreateHotelAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateHotelAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateHotelAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
