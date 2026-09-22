package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * PLAN_V22 P2.1 — the active-account pointer must be a real, persisted single source of truth
 * instead of living only in `ProfileUiState.selectedAccountId` (an in-memory VM field lost on
 * process death). [FakeActiveAccountSource] stands in for the DataStore-backed
 * `ActiveAccountStore` (real DataStore round-trip is exercised via instrumentation, mirroring how
 * `SessionRepository` is tested — see `SessionStateTest`'s doc comment).
 */
class ActiveAccountStoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeDemoSettingsRepository() = mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) }

    // P3.2: ProfileViewModel now collects `sessionState.first()` in init(); a relaxed mockk's
    // auto-generated Flow<SessionState> never emits (null-collector trap), so it's stubbed here.
    private fun fakeSessionRepository() = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) }

    @Test
    fun `get-set round-trip is empty until a value is persisted`() =
        runTest {
            val source = FakeActiveAccountSource()
            assertNull(source.activeAccountId.first())

            source.setActiveAccountId("ACC-002")

            assertEquals("ACC-002", source.activeAccountId.first())
        }

    @Test
    fun `set overwrites a previously persisted value`() =
        runTest {
            val source = FakeActiveAccountSource(seed = "ACC-001")
            assertEquals("ACC-001", source.activeAccountId.first())

            source.setActiveAccountId("ACC-003")

            assertEquals("ACC-003", source.activeAccountId.first())
        }

    @Test
    fun `ProfileViewModel adopts the persisted active account on init instead of defaulting to the first`() =
        runTest {
            // Seeded demo accounts are ACC-001..ACC-003 (see FakeProfileRepository); a prior
            // process would have persisted a non-first persona as active before dying.
            val vm =
                ProfileViewModel(
                    FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
                    ThemeController(),
                    FakeActiveAccountSource(seed = "ACC-002"),
                    fakeDemoSettingsRepository(),
                    fakeSessionRepository(),
                )
            advanceUntilIdle()

            assertEquals(
                "ACC-002",
                vm.uiState.value.selectedAccountId,
                "the persisted pointer must survive process death, not reset to the first seeded account",
            )
        }

    @Test
    fun `ProfileViewModel falls back to the first seeded account when nothing was ever persisted`() =
        runTest {
            val vm =
                ProfileViewModel(
                    FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
                    ThemeController(),
                    FakeActiveAccountSource(seed = null),
                    fakeDemoSettingsRepository(),
                    fakeSessionRepository(),
                )
            advanceUntilIdle()

            assertEquals("ACC-001", vm.uiState.value.selectedAccountId)
        }

    @Test
    fun `ProfileViewModel ignores a persisted id that no longer exists in the account list`() =
        runTest {
            val vm =
                ProfileViewModel(
                    FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
                    ThemeController(),
                    FakeActiveAccountSource(seed = "ACC-DELETED"),
                    fakeDemoSettingsRepository(),
                    fakeSessionRepository(),
                )
            advanceUntilIdle()

            assertEquals(
                "ACC-001",
                vm.uiState.value.selectedAccountId,
                "a stale persisted id (e.g. its account was later removed) must fall back safely",
            )
        }
}
