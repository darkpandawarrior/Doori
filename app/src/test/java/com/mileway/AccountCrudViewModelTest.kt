package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.viewmodel.ProfileAction
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P1.3: covers `ProfileAction.AddDemoAccount`/`RemoveDemoAccount`/`ViewAccountDetails` — the
 * add/remove/details CRUD affordances added to `PersonaSwitcherRow`, including the two guard
 * branches (can't remove the active persona, can't remove the last remaining one).
 */
class AccountCrudViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(): ProfileViewModel {
        val vm =
            ProfileViewModel(
                FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
                ThemeController(),
                FakeActiveAccountSource(),
                mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) },
                mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) },
            )
        return vm
    }

    @Test
    fun `adding a persona persists and appears in the row`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle() // let init's seedAccountsIfEmpty + observeAccounts collect land

            vm.onAction(ProfileAction.AddDemoAccount("New Persona", "EMP999", "New Org"))
            advanceUntilIdle()

            val accounts = vm.uiState.value.accounts
            assertEquals(4, accounts.size)
            assertTrue(accounts.any { it.displayName == "New Persona" && it.employeeCode == "EMP999" })
        }

    @Test
    fun `removing a non-active non-last persona removes it from the row`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            // Seeded active persona is ACC-001; ACC-002 is a safe non-active, non-last target.
            assertEquals("ACC-001", vm.uiState.value.selectedAccountId)

            vm.onAction(ProfileAction.RemoveDemoAccount("ACC-002"))
            advanceUntilIdle()

            val accounts = vm.uiState.value.accounts
            assertEquals(2, accounts.size)
            assertTrue(accounts.none { it.id == "ACC-002" })
            assertNull(vm.uiState.value.preferenceMessage)
        }

    @Test
    fun `removing the active persona is a no-op with a snackbar`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            val activeId = vm.uiState.value.selectedAccountId

            vm.onAction(ProfileAction.RemoveDemoAccount(activeId))
            advanceUntilIdle()

            assertEquals(3, vm.uiState.value.accounts.size, "active persona must not be removed")
            assertTrue(
                vm.uiState.value.accounts
                    .any { it.id == activeId },
            )
            assertTrue(
                vm.uiState.value.preferenceMessage
                    .orEmpty()
                    .isNotBlank(),
            )
        }

    @Test
    fun `removing the last remaining persona is a no-op with a snackbar`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            // Drain down to a single, non-active persona first.
            vm.onAction(ProfileAction.RemoveDemoAccount("ACC-002"))
            advanceUntilIdle()
            vm.onAction(ProfileAction.SwitchAccount("ACC-001"))
            vm.onAction(ProfileAction.RemoveDemoAccount("ACC-003"))
            advanceUntilIdle()
            assertEquals(1, vm.uiState.value.accounts.size)

            vm.onAction(ProfileAction.ClearPreferenceMessage)
            vm.onAction(ProfileAction.RemoveDemoAccount("ACC-001"))
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.accounts.size, "the only remaining persona must not be removed")
            assertTrue(
                vm.uiState.value.preferenceMessage
                    .orEmpty()
                    .isNotBlank(),
            )
        }

    @Test
    fun `viewing then dismissing account details toggles the sheet state`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            val id =
                vm.uiState.value.accounts
                    .first()
                    .id

            vm.onAction(ProfileAction.ViewAccountDetails(id))
            assertEquals(
                id,
                vm.uiState.value.accountDetailsSheet
                    ?.id,
            )

            vm.onAction(ProfileAction.DismissAccountDetails)
            assertNull(vm.uiState.value.accountDetailsSheet)
        }
}
