package com.mileway

import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.feature.profile.viewmodel.PhoneChangeError
import com.mileway.feature.profile.viewmodel.PhoneChangeStep
import com.mileway.feature.profile.viewmodel.PhoneChangeViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V24 P3.1 — phone change with OTP re-verify: validation, send→verify→commit, and the
 * wrong-code branch, over a real [LocalOtpEngine]. (In app/src/test because SessionRepository is a
 * platform class mocked with mockk, which feature:profile commonTest lacks.)
 */
class PhoneChangeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun session(state: SessionState = SessionState()): SessionRepository =
        mockk(relaxed = true) { every { sessionState } returns MutableStateFlow(state) }

    @Test
    fun `invalid phone is rejected before any OTP`() =
        runTest {
            val vm = PhoneChangeViewModel(session(), LocalOtpEngine())
            advanceUntilIdle()
            vm.onNewPhoneChange("12345")
            vm.requestOtp()
            advanceUntilIdle()
            assertEquals(PhoneChangeError.INVALID_PHONE, vm.state.value.error)
            assertEquals(PhoneChangeStep.ENTER_PHONE, vm.state.value.step)
        }

    @Test
    fun `valid phone sends an OTP and moves to verify`() =
        runTest {
            val repo = session()
            val vm = PhoneChangeViewModel(repo, LocalOtpEngine())
            advanceUntilIdle()
            vm.onNewPhoneChange("9876543210")
            vm.requestOtp()
            advanceUntilIdle()

            assertEquals(PhoneChangeStep.VERIFY, vm.state.value.step)
            assertTrue(vm.state.value.delivery != null)
            coVerify { repo.startPhoneChange("9876543210") }
        }

    @Test
    fun `correct code commits the change`() =
        runTest {
            val repo = session()
            val engine = LocalOtpEngine()
            val vm = PhoneChangeViewModel(repo, engine)
            advanceUntilIdle()
            vm.onNewPhoneChange("9876543210")
            vm.requestOtp()
            advanceUntilIdle()

            vm.onCodeChange(engine.codeFor(OtpPurpose.PHONE_CHANGE, "9876543210"))
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            coVerify { repo.commitPhoneChange() }
        }

    @Test
    fun `wrong code surfaces an error and does not commit`() =
        runTest {
            val repo = session()
            val engine = LocalOtpEngine()
            val vm = PhoneChangeViewModel(repo, engine)
            advanceUntilIdle()
            vm.onNewPhoneChange("9876543210")
            vm.requestOtp()
            advanceUntilIdle()

            val correct = engine.codeFor(OtpPurpose.PHONE_CHANGE, "9876543210")
            val wrong = if (correct == "000000") "111111" else "000000"
            vm.onCodeChange(wrong)
            advanceUntilIdle()

            assertEquals(PhoneChangeError.WRONG_CODE, vm.state.value.error)
            assertTrue(!vm.state.value.done)
        }
}
