package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.viewmodel.CreateMjpAction
import com.miletracker.feature.travel.viewmodel.CreateMjpViewModel
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TR.6 (V17): the multi-city Journey-Plan reducer — add/remove legs, canSubmit gating (purpose + every leg
 * complete) + the rotating result paths through the shared FormSubmissionScaffold contract.
 */
class CreateMjpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateMjpViewModel(TravelCreateRepository())

    private fun completeLeg(vm: CreateMjpViewModel, index: Int) {
        vm.onAction(CreateMjpAction.SetLegFrom(index, "Pune"))
        vm.onAction(CreateMjpAction.SetLegTo(index, "Delhi"))
        vm.onAction(CreateMjpAction.SetLegDate(index, "12-07-2026"))
    }

    @Test
    fun `add and remove legs keeps at least one leg`() {
        val vm = viewModel()
        assertEquals(1, vm.state.value.legs.size)
        vm.onAction(CreateMjpAction.AddLeg)
        assertEquals(2, vm.state.value.legs.size)
        vm.onAction(CreateMjpAction.RemoveLeg(1))
        assertEquals(1, vm.state.value.legs.size)
        vm.onAction(CreateMjpAction.RemoveLeg(0)) // cannot drop the last one
        assertEquals(1, vm.state.value.legs.size)
    }

    @Test
    fun `canSubmit requires a purpose and every leg complete`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateMjpAction.SetPurpose("Roadshow"))
        completeLeg(vm, 0)
        assertTrue(vm.state.value.canSubmit)
        vm.onAction(CreateMjpAction.AddLeg) // a fresh, incomplete leg blocks submit again
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateMjpAction.SetPurpose("Roadshow"))
        completeLeg(vm, 0)

        vm.effect.test {
            vm.onAction(CreateMjpAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Success)
            vm.onAction(CreateMjpAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.NeedsApproval)
            vm.onAction(CreateMjpAction.Submit)
            assertTrue(awaitItem() is TravelCreateEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
