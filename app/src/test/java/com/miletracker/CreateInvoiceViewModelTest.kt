package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.payables.repository.InvoiceRepository
import com.miletracker.feature.payables.viewmodel.CreateInvoiceAction
import com.miletracker.feature.payables.viewmodel.CreateInvoiceEffect
import com.miletracker.feature.payables.viewmodel.CreateInvoiceViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PB.1 (V17): the Create-Invoice reducer, canSubmit gating + the rotating success / approval / violation
 * result paths through the FormSubmissionScaffold contract.
 */
class CreateInvoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateInvoiceViewModel(InvoiceRepository())

    @Test
    fun `canSubmit is false until the required fields and a positive amount are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-1"))
        vm.onAction(CreateInvoiceAction.SetVendor("Acme"))
        assertFalse(vm.state.value.canSubmit) // amount still missing
        vm.onAction(CreateInvoiceAction.SetAmount("1200"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-1"))
        vm.onAction(CreateInvoiceAction.SetVendor("Acme"))
        vm.onAction(CreateInvoiceAction.SetAmount("1200"))

        vm.effect.test {
            vm.onAction(CreateInvoiceAction.Submit)
            assertTrue(awaitItem() is CreateInvoiceEffect.Success)
            vm.onAction(CreateInvoiceAction.Submit)
            assertTrue(awaitItem() is CreateInvoiceEffect.NeedsApproval)
            vm.onAction(CreateInvoiceAction.Submit)
            assertTrue(awaitItem() is CreateInvoiceEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
