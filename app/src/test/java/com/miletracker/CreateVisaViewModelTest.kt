package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateVisaAction
import com.miletracker.feature.travel.viewmodel.CreateVisaViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.7 (V17): the Visa-request reducer — canSubmit gating (country + passport + travel date) + the rotating
 * success / approval / violation result paths through the shared FormSubmissionScaffold contract.
 */
class CreateVisaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateVisaViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until country, passport and travel date are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateVisaAction.SetCountry("Singapore"))
        vm.onAction(CreateVisaAction.SetPassportNumber("Z1234567"))
        assertFalse(vm.state.value.canSubmit) // travel date still missing
        vm.onAction(CreateVisaAction.SetTravelDate("20-09-2026"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateVisaAction.SetCountry("Singapore"))
        vm.onAction(CreateVisaAction.SetPassportNumber("Z1234567"))
        vm.onAction(CreateVisaAction.SetTravelDate("20-09-2026"))

        vm.effect.test {
            vm.onAction(CreateVisaAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateVisaAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateVisaAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
