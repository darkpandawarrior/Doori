package com.mileway.feature.payables.viewmodel

import com.mileway.feature.payables.repository.ParkMode
import com.mileway.feature.payables.repository.ParkingRepository
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
 * PB.3: covers [CreateParkingViewModel] — the [CreateParkingUiState.canSubmit] validation gate, the
 * [ParkMode] segment feeding the submitted id prefix, and the rotating three-way submit result
 * ([ParkingRepository] cycles Submitted / NeedsApproval / PolicyViolation).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateParkingViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: ParkingRepository = ParkingRepository()) = CreateParkingViewModel(repository)

    @Test
    fun `canSubmit is false until vehicle number and gate are both set`() {
        val vm = newViewModel()
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
        assertFalse(vm.state.value.canSubmit) // gate still blank

        vm.onAction(CreateParkingAction.SetGate("Gate 2"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `driver name and PO reference are optional and never gate submit`() {
        val vm = newViewModel()
        vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
        vm.onAction(CreateParkingAction.SetGate("Gate 2"))

        assertTrue(vm.state.value.canSubmit)
        assertEquals("", vm.state.value.driverName)
        assertEquals("", vm.state.value.poReference)
    }

    @Test
    fun `submit is a no-op while canSubmit is false`() =
        runTest {
            val repository = ParkingRepository()
            val vm = newViewModel(repository)
            vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234")) // gate still missing

            vm.onAction(CreateParkingAction.Submit)

            assertEquals(0, repository.count())
            assertEquals(null, vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)
        }

    @Test
    fun `Park In mode mints a PIN-prefixed id on a successful submit`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
            vm.onAction(CreateParkingAction.SetGate("Gate 2"))
            assertEquals(ParkMode.IN, vm.state.value.mode) // default mode

            vm.onAction(CreateParkingAction.Submit)
            val effect = vm.effect.first()

            assertIs<CreateParkingEffect.Success>(effect)
            assertTrue(effect.id.startsWith("PIN-"))
        }

    @Test
    fun `Park Out mode mints a POUT-prefixed id on a successful submit`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(CreateParkingAction.SetMode(ParkMode.OUT))
            vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
            vm.onAction(CreateParkingAction.SetGate("Gate 2"))

            vm.onAction(CreateParkingAction.Submit)
            val effect = vm.effect.first()

            assertIs<CreateParkingEffect.Success>(effect)
            assertTrue(effect.id.startsWith("POUT-"))
        }

    @Test
    fun `three consecutive submits rotate through submitted, needs-approval and policy-violation`() =
        runTest {
            val repository = ParkingRepository()
            val vm = newViewModel(repository)
            vm.onAction(CreateParkingAction.SetVehicleNumber("MH12AB1234"))
            vm.onAction(CreateParkingAction.SetGate("Gate 2"))

            vm.onAction(CreateParkingAction.Submit)
            val first = vm.effect.first()
            assertIs<CreateParkingEffect.Success>(first)
            assertIs<PayablesSubmissionResult.Submitted>(vm.state.value.lastResult)

            vm.onAction(CreateParkingAction.Submit)
            val second = vm.effect.first()
            assertIs<CreateParkingEffect.NeedsApproval>(second)
            assertIs<PayablesSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateParkingAction.Submit)
            val third = vm.effect.first()
            assertIs<CreateParkingEffect.Violation>(third)
            assertEquals(
                listOf("Vehicle number not on the approved list", "Gate event needs security clearance"),
                third.messages,
            )
            assertIs<PayablesSubmissionResult.PolicyViolation>(vm.state.value.lastResult)

            assertEquals(3, repository.count())
        }
}
