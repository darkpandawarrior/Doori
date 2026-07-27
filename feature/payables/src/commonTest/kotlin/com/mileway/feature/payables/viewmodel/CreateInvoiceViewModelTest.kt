package com.mileway.feature.payables.viewmodel

import com.mileway.feature.payables.repository.InvoiceRepository
import com.mileway.feature.payables.repository.InvoiceSubmissionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * PB.1: covers [CreateInvoiceViewModel] — the [CreateInvoiceUiState.canSubmit] validation gate and the
 * rotating three-way submit result ([InvoiceRepository] cycles Submitted / NeedsApproval / PolicyViolation).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateInvoiceViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: InvoiceRepository = InvoiceRepository()) = CreateInvoiceViewModel(repository)

    private fun fillValidForm(vm: CreateInvoiceViewModel) {
        vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-100"))
        vm.onAction(CreateInvoiceAction.SetVendor("Acme Traders"))
        vm.onAction(CreateInvoiceAction.SetAmount("500"))
    }

    @Test
    fun `canSubmit is false until invoice number, vendor and a positive amount are all set`() {
        val vm = newViewModel()
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-100"))
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateInvoiceAction.SetVendor("Acme Traders"))
        assertFalse(vm.state.value.canSubmit) // amount still blank

        vm.onAction(CreateInvoiceAction.SetAmount("0"))
        assertFalse(vm.state.value.canSubmit) // zero amount does not count as positive

        vm.onAction(CreateInvoiceAction.SetAmount("500"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `a non-numeric amount is treated as no amount and blocks submit`() {
        val vm = newViewModel()
        vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-100"))
        vm.onAction(CreateInvoiceAction.SetVendor("Acme Traders"))
        vm.onAction(CreateInvoiceAction.SetAmount("not-a-number"))

        assertFalse(vm.state.value.canSubmit)
        assertEquals(null, vm.state.value.amount)
    }

    @Test
    fun `submit is a no-op while canSubmit is false`() =
        runTest {
            val repository = InvoiceRepository()
            val vm = newViewModel(repository)
            vm.onAction(CreateInvoiceAction.SetInvoiceNumber("INV-100")) // vendor + amount still missing

            vm.onAction(CreateInvoiceAction.Submit)

            assertEquals(0, repository.count())
            assertEquals(null, vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)
        }

    @Test
    fun `three consecutive submits rotate through submitted, needs-approval and policy-violation`() =
        runTest {
            val repository = InvoiceRepository()
            val vm = newViewModel(repository)
            fillValidForm(vm)

            vm.onAction(CreateInvoiceAction.Submit)
            val first = vm.effect.first()
            assertIs<CreateInvoiceEffect.Success>(first)
            assertIs<InvoiceSubmissionResult.Submitted>(vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)

            vm.onAction(CreateInvoiceAction.Submit)
            val second = vm.effect.first()
            assertIs<CreateInvoiceEffect.NeedsApproval>(second)
            assertIs<InvoiceSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateInvoiceAction.Submit)
            val third = vm.effect.first()
            assertIs<CreateInvoiceEffect.Violation>(third)
            assertEquals(
                listOf("Amount exceeds the auto-approval limit", "GL code requires finance review"),
                third.messages,
            )
            assertIs<InvoiceSubmissionResult.PolicyViolation>(vm.state.value.lastResult)

            assertEquals(3, repository.count())
        }
}
