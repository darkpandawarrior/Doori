package com.mileway

import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.feature.cards.viewmodel.CardKycAction
import com.mileway.feature.cards.viewmodel.CardKycViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P4.3: covers [CardKycViewModel]'s 5-step machine — the per-step advance gate, the OTP
 * send/verify branch (via the real [LocalOtpEngine]), and the simulated-processing → done finish.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardKycViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newVm() = CardKycViewModel(LocalOtpEngine())

    @Test
    fun `intro advances but personal info gates on all fields`() =
        runTest {
            val vm = newVm()
            vm.onAction(CardKycAction.Next) // 0 -> 1
            advanceUntilIdle()
            assertEquals(1, vm.state.value.step)

            // Blank personal info: Next is a no-op.
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertEquals(1, vm.state.value.step)

            vm.onAction(CardKycAction.SetFullName("Asha Verma"))
            vm.onAction(CardKycAction.SetIdNumber("ID-123"))
            vm.onAction(CardKycAction.SetPhone("9876543210"))
            vm.onAction(CardKycAction.Next) // 1 -> 2, sends OTP
            advanceUntilIdle()

            assertEquals(2, vm.state.value.step)
            assertEquals("9876543210", vm.state.value.otpSentTo)
            assertTrue(vm.state.value.demoCode?.length == 6)
        }

    @Test
    fun `a wrong OTP is rejected and the correct one advances`() =
        runTest {
            val vm = newVm()
            vm.onAction(CardKycAction.Next)
            vm.onAction(CardKycAction.SetFullName("Asha"))
            vm.onAction(CardKycAction.SetIdNumber("ID-1"))
            vm.onAction(CardKycAction.SetPhone("9876543210"))
            vm.onAction(CardKycAction.Next) // -> step 2
            advanceUntilIdle()

            vm.onAction(CardKycAction.SetOtp("000000"))
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertTrue(vm.state.value.otpError)
            assertEquals(2, vm.state.value.step)

            val code = vm.state.value.demoCode!!
            vm.onAction(CardKycAction.SetOtp(code))
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertFalse(vm.state.value.otpError)
            assertEquals(3, vm.state.value.step)
        }

    @Test
    fun `document and selfie steps gate on attach, and final submit finishes`() =
        runTest {
            val vm = newVm()
            // Drive through to step 3.
            vm.onAction(CardKycAction.Next)
            vm.onAction(CardKycAction.SetFullName("Asha"))
            vm.onAction(CardKycAction.SetIdNumber("ID-1"))
            vm.onAction(CardKycAction.SetPhone("9876543210"))
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            vm.onAction(CardKycAction.SetOtp(vm.state.value.demoCode!!))
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertEquals(3, vm.state.value.step)

            // Doc step gates on attach.
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertEquals(3, vm.state.value.step)
            vm.onAction(CardKycAction.AttachDocument)
            vm.onAction(CardKycAction.Next) // -> step 4
            advanceUntilIdle()
            assertEquals(4, vm.state.value.step)

            // Selfie step gates on capture, then final submit finishes.
            vm.onAction(CardKycAction.Next)
            advanceUntilIdle()
            assertFalse(vm.state.value.done)
            vm.onAction(CardKycAction.AttachSelfie)
            vm.onAction(CardKycAction.Next) // final submit
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
        }
}
