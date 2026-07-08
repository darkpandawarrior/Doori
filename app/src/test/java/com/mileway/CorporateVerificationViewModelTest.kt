package com.mileway

import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.feature.profile.viewmodel.CorporateError
import com.mileway.feature.profile.viewmodel.CorporateStep
import com.mileway.feature.profile.viewmodel.CorporateVerificationViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * PLAN_V24 P4.4 — corporate email verification: domain validation, OTP send→verify, and the
 * wrong-code branch, over a real [LocalOtpEngine]. (In app/src/test because SessionRepository is a
 * platform class mocked with mockk, which feature:profile commonTest lacks.)
 */
class CorporateVerificationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun session(state: SessionState = SessionState()): SessionRepository =
        mockk(relaxed = true) { io.mockk.every { sessionState } returns MutableStateFlow(state) }

    @Test
    fun `an unrecognised domain is rejected before any OTP`() =
        runTest {
            val vm = CorporateVerificationViewModel(session(), LocalOtpEngine())
            advanceUntilIdle()
            vm.onEmailChange("someone@gmail.com")
            vm.requestOtp()
            advanceUntilIdle()

            assertEquals(CorporateError.UNRECOGNISED_DOMAIN, vm.state.value.error)
            assertEquals(CorporateStep.ENTER_EMAIL, vm.state.value.step)
        }

    @Test
    fun `a recognised domain sends an OTP and moves to verify`() =
        runTest {
            val vm = CorporateVerificationViewModel(session(), LocalOtpEngine())
            advanceUntilIdle()
            vm.onEmailChange("asha@acmecorp.com")
            vm.requestOtp()
            advanceUntilIdle()

            assertEquals(CorporateStep.VERIFY, vm.state.value.step)
            assertEquals(6, vm.state.value.demoCode?.length)
        }

    @Test
    fun `a wrong code surfaces an error and does not verify`() =
        runTest {
            val repo = session()
            val vm = CorporateVerificationViewModel(repo, LocalOtpEngine())
            advanceUntilIdle()
            vm.onEmailChange("asha@acmecorp.com")
            vm.requestOtp()
            advanceUntilIdle()

            vm.onCodeChange("000000")
            advanceUntilIdle()

            assertEquals(CorporateError.WRONG_CODE, vm.state.value.error)
            coVerify(exactly = 0) { repo.markCorporateVerified(any()) }
        }

    @Test
    fun `the correct code marks the corporate email verified`() =
        runTest {
            val repo = session()
            val vm = CorporateVerificationViewModel(repo, LocalOtpEngine())
            advanceUntilIdle()
            vm.onEmailChange("asha@acmecorp.com")
            vm.requestOtp()
            advanceUntilIdle()

            vm.onCodeChange(vm.state.value.demoCode!!)
            advanceUntilIdle()

            assertNull(vm.state.value.error)
            coVerify { repo.markCorporateVerified("asha@acmecorp.com") }
        }
}
