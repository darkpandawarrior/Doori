package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.payments.model.PaymentDirection
import com.miletracker.feature.payments.repository.PaymentsRepository
import com.miletracker.feature.payments.viewmodel.CreatePaymentAction
import com.miletracker.feature.payments.viewmodel.CreatePaymentEffect
import com.miletracker.feature.payments.viewmodel.CreatePaymentViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PM (V17): the Create-Payment reducer, direction toggle, canSubmit gating (counterparty + positive amount) +
 * the rotating completed / pending / failed result paths through the FormSubmissionScaffold contract.
 */
class CreatePaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreatePaymentViewModel(PaymentsRepository())

    @Test
    fun `direction defaults to Pay and can switch to Request`() {
        val vm = viewModel()
        assertEquals(PaymentDirection.PAY, vm.state.value.direction)
        vm.onAction(CreatePaymentAction.SetDirection(PaymentDirection.REQUEST))
        assertEquals(PaymentDirection.REQUEST, vm.state.value.direction)
    }

    @Test
    fun `canSubmit is false until counterparty and a positive amount are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreatePaymentAction.SetCounterparty("chai@stall"))
        assertFalse(vm.state.value.canSubmit) // amount still missing
        vm.onAction(CreatePaymentAction.SetAmount("60"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through completed, pending and failed`() = runTest {
        val vm = viewModel()
        vm.onAction(CreatePaymentAction.SetCounterparty("chai@stall"))
        vm.onAction(CreatePaymentAction.SetAmount("60"))

        vm.effect.test {
            vm.onAction(CreatePaymentAction.Submit)
            assertTrue(awaitItem() is CreatePaymentEffect.Completed)
            vm.onAction(CreatePaymentAction.Submit)
            assertTrue(awaitItem() is CreatePaymentEffect.Pending)
            vm.onAction(CreatePaymentAction.Submit)
            assertTrue(awaitItem() is CreatePaymentEffect.Failed)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
