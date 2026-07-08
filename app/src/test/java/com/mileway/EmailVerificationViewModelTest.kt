package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.feature.profile.viewmodel.EmailVerificationViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P3.2 — email verification status: unverified shows the verify path, sending a link
 * surfaces the simulate-click action, and confirming marks the email verified.
 */
class EmailVerificationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun repo(state: SessionState): SessionRepository =
        mockk(relaxed = true) { every { sessionState } returns MutableStateFlow(state) }

    @Test
    fun `unverified email exposes the verify flow`() =
        runTest {
            val vm = EmailVerificationViewModel(repo(SessionState(email = "a@b.com", emailVerified = false)))
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isVerified)

            vm.sendLink()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.linkSent)
        }

    @Test
    fun `already-verified email reports verified`() =
        runTest {
            val vm = EmailVerificationViewModel(repo(SessionState(email = "a@b.com", emailVerified = true)))
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isVerified)
        }

    @Test
    fun `simulate click marks the email verified`() =
        runTest {
            val session = repo(SessionState(email = "a@b.com", emailVerified = false))
            val vm = EmailVerificationViewModel(session)
            advanceUntilIdle()
            vm.sendLink()
            vm.confirmClicked()
            advanceUntilIdle()
            coVerify { session.markEmailVerified() }
        }
}
