package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.payables.repository.GinRepository
import com.miletracker.feature.payables.viewmodel.CreateGinAction
import com.miletracker.feature.payables.viewmodel.CreateGinEffect
import com.miletracker.feature.payables.viewmodel.CreateGinViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PB.2 (V17): the Create-GIN reducer, canSubmit gating (GIN number + PO reference + positive received qty) +
 * the rotating success / approval / violation result paths through the FormSubmissionScaffold contract.
 */
class CreateGinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateGinViewModel(GinRepository())

    @Test
    fun `canSubmit is false until the required fields and a positive quantity are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateGinAction.SetGinNumber("GIN-1"))
        vm.onAction(CreateGinAction.SetPoReference("PO-9"))
        assertFalse(vm.state.value.canSubmit) // quantity still missing
        vm.onAction(CreateGinAction.SetReceivedQty("12"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateGinAction.SetGinNumber("GIN-1"))
        vm.onAction(CreateGinAction.SetPoReference("PO-9"))
        vm.onAction(CreateGinAction.SetReceivedQty("12"))

        vm.effect.test {
            vm.onAction(CreateGinAction.Submit)
            assertTrue(awaitItem() is CreateGinEffect.Success)
            vm.onAction(CreateGinAction.Submit)
            assertTrue(awaitItem() is CreateGinEffect.NeedsApproval)
            vm.onAction(CreateGinAction.Submit)
            assertTrue(awaitItem() is CreateGinEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
