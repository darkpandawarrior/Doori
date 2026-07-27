package com.mileway.feature.payables.viewmodel

import com.mileway.feature.payables.repository.GinRepository
import com.mileway.feature.payables.repository.PayablesSubmissionResult
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
 * PB.2: covers [CreateGinViewModel] — the [CreateGinUiState.canSubmit] validation gate and the rotating
 * three-way submit result ([GinRepository] cycles Submitted / NeedsApproval / PolicyViolation).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateGinViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: GinRepository = GinRepository()) = CreateGinViewModel(repository)

    private fun fillValidForm(vm: CreateGinViewModel) {
        vm.onAction(CreateGinAction.SetGinNumber("GIN-100"))
        vm.onAction(CreateGinAction.SetPoReference("PO-2024-001"))
        vm.onAction(CreateGinAction.SetReceivedQty("10"))
    }

    @Test
    fun `canSubmit is false until gin number, PO reference and a positive received qty are all set`() {
        val vm = newViewModel()
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateGinAction.SetGinNumber("GIN-100"))
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateGinAction.SetPoReference("PO-2024-001"))
        assertFalse(vm.state.value.canSubmit) // received qty still blank

        vm.onAction(CreateGinAction.SetReceivedQty("0"))
        assertFalse(vm.state.value.canSubmit) // zero qty does not count as positive

        vm.onAction(CreateGinAction.SetReceivedQty("10"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `a non-numeric received qty is treated as no qty and blocks submit`() {
        val vm = newViewModel()
        vm.onAction(CreateGinAction.SetGinNumber("GIN-100"))
        vm.onAction(CreateGinAction.SetPoReference("PO-2024-001"))
        vm.onAction(CreateGinAction.SetReceivedQty("ten"))

        assertFalse(vm.state.value.canSubmit)
        assertEquals(null, vm.state.value.receivedQty)
    }

    @Test
    fun `submit is a no-op while canSubmit is false`() =
        runTest {
            val repository = GinRepository()
            val vm = newViewModel(repository)
            vm.onAction(CreateGinAction.SetGinNumber("GIN-100")) // PO ref + qty still missing

            vm.onAction(CreateGinAction.Submit)

            assertEquals(0, repository.count())
            assertEquals(null, vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)
        }

    @Test
    fun `three consecutive submits rotate through submitted, needs-approval and policy-violation`() =
        runTest {
            val repository = GinRepository()
            val vm = newViewModel(repository)
            fillValidForm(vm)

            vm.onAction(CreateGinAction.Submit)
            val first = vm.effect.first()
            assertIs<CreateGinEffect.Success>(first)
            assertIs<PayablesSubmissionResult.Submitted>(vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)

            vm.onAction(CreateGinAction.Submit)
            val second = vm.effect.first()
            assertIs<CreateGinEffect.NeedsApproval>(second)
            assertIs<PayablesSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateGinAction.Submit)
            val third = vm.effect.first()
            assertIs<CreateGinEffect.Violation>(third)
            assertEquals(
                listOf("Received quantity exceeds the PO quantity", "Goods receipt requires QC clearance"),
                third.messages,
            )
            assertIs<PayablesSubmissionResult.PolicyViolation>(vm.state.value.lastResult)

            assertEquals(3, repository.count())
        }
}
