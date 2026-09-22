package com.mileway

import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.ui.home.FirstLoginBannerViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V22 P7.1 — the welcome banner must be gated by [SessionState.isFirstLoginPending] and
 * show exactly once per fresh sign-in: visible while the flag is true, cleared via
 * [SessionRepository.clearFirstLoginPending] once [FirstLoginBannerViewModel.onBannerShown] runs,
 * and never set back to true again until the next fresh `signInWithCredentials` call.
 */
class FirstLoginBannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `banner is visible and carries the synthesized profile when isFirstLoginPending is true`() =
        runTest {
            val sessionFlow =
                MutableStateFlow(
                    SessionState(
                        kind = SessionKind.CREDENTIALS,
                        email = "demo@mileway.app",
                        displayName = "Demo User",
                        officeName = "Demo HQ",
                        isFirstLoginPending = true,
                    ),
                )
            val sessionRepository = mockk<SessionRepository> { every { sessionState } returns sessionFlow }
            val vm = FirstLoginBannerViewModel(sessionRepository)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isVisible)
            assertEquals("Demo User", vm.uiState.value.displayName)
            assertEquals("Demo HQ", vm.uiState.value.officeName)
        }

    @Test
    fun `banner is not visible when isFirstLoginPending is false`() =
        runTest {
            val sessionFlow = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, isFirstLoginPending = false))
            val sessionRepository = mockk<SessionRepository> { every { sessionState } returns sessionFlow }
            val vm = FirstLoginBannerViewModel(sessionRepository)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isVisible)
        }

    @Test
    fun `onBannerShown clears the one-shot flag exactly once`() =
        runTest {
            val sessionFlow = MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, isFirstLoginPending = true))
            val sessionRepository =
                mockk<SessionRepository> {
                    every { sessionState } returns sessionFlow
                    coEvery { clearFirstLoginPending() } answers { sessionFlow.value = sessionFlow.value.copy(isFirstLoginPending = false) }
                }
            val vm = FirstLoginBannerViewModel(sessionRepository)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isVisible, "banner must be visible before it's cleared")

            vm.onBannerShown()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isVisible, "the flag must clear after the banner is shown")

            // Never reappears on its own — nothing but a fresh sign-in flips it back to true.
            vm.onBannerShown()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isVisible)
        }
}
