package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.viewmodel.ProfileAction
import com.mileway.feature.profile.viewmodel.ProfileEffect
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V22 P2.4 — `ProfileAction.SignOut` has-other-accounts vs no-other-accounts branching:
 * signing out a non-last persona removes it and switches to another remaining one; signing out
 * the last persona clears the whole local session (`SessionRepository.signOut()`, which existed
 * but had zero call sites before this task) and emits `ProfileEffect.NavigateToLogin`.
 */
class SignOutTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeDemoSettingsRepository() = mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) }

    // P3.2: ProfileViewModel now collects `sessionState.first()` in init(); a relaxed mockk's
    // auto-generated Flow<SessionState> never emits (null-collector trap), so it's stubbed here.
    private fun fakeSessionRepository() = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) }

    private fun viewModel(
        mockAccountRepository: MockAccountRepository = MockAccountRepository(FakeMockAccountDao()),
        sessionRepository: SessionRepository = fakeSessionRepository(),
    ): ProfileViewModel =
        ProfileViewModel(
            FakeProfileRepository(mockAccountRepository),
            ThemeController(),
            FakeActiveAccountSource(seed = "ACC-001"),
            fakeDemoSettingsRepository(),
            sessionRepository,
        )

    @Test
    fun `signing out a non-last persona removes it and switches to another remaining one`() =
        runTest {
            val sessionRepository = fakeSessionRepository()
            val vm = viewModel(sessionRepository = sessionRepository)
            advanceUntilIdle()
            assertEquals("ACC-001", vm.uiState.value.selectedAccountId)

            vm.onAction(ProfileAction.SignOut("ACC-001"))
            advanceUntilIdle()

            val accounts = vm.uiState.value.accounts
            assertTrue(accounts.none { it.id == "ACC-001" }, "the signed-out persona must be removed")
            assertTrue(accounts.isNotEmpty(), "at least one persona must remain (3 seeded, 1 removed)")
            assertTrue(
                vm.uiState.value.selectedAccountId != "ACC-001",
                "the active pointer must move to a remaining persona",
            )
            coVerify(exactly = 0) { sessionRepository.signOut() }
        }

    @Test
    fun `signing out the last remaining persona clears the session and emits NavigateToLogin`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val mockAccountRepository = MockAccountRepository(accountDao)
            val sessionRepository = fakeSessionRepository()
            val vm = viewModel(mockAccountRepository = mockAccountRepository, sessionRepository = sessionRepository)
            advanceUntilIdle()

            // Drain down to a single remaining persona first (3 seeded personas: ACC-001..003).
            vm.onAction(ProfileAction.SignOut("ACC-001"))
            advanceUntilIdle()
            vm.onAction(ProfileAction.SignOut("ACC-002"))
            advanceUntilIdle()
            val onlyRemaining =
                vm.uiState.value.accounts
                    .single()
                    .id

            vm.onAction(ProfileAction.SignOut(onlyRemaining))
            advanceUntilIdle()

            assertTrue(
                vm.uiState.value.accounts
                    .isEmpty(),
                "the last persona must actually be removed",
            )
            coVerify(exactly = 1) { sessionRepository.signOut() }
            assertEquals(ProfileEffect.NavigateToLogin, vm.effect.first())
        }
}
