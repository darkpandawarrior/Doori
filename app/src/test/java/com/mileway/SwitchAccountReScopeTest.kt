package com.mileway

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.viewmodel.ProfileAction
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
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
 * PLAN_V22 P2.2 — `SwitchAccount` must be a real switch, not a cosmetic UI-state flag: it has to
 * (1) persist the choice to [com.mileway.core.data.session.ActiveAccountSource] so it survives
 * process death (reusing P2.1's store) and (2) drive a re-query of account-scoped data, proven
 * here end-to-end through [SavedTracksViewModel] re-scoping Journeys/Expenses by
 * `started_by_account_id`.
 *
 * P2.3 gates `SwitchAccount` behind a PIN/biometric confirmation (see `SwitchAccountViewModelTest`
 * for that gate's own coverage) — these tests dispatch `CommitAccountSwitch` directly to simulate
 * a gate that already succeeded, since re-scoping is the behavior under test here, not the gate.
 */
class SwitchAccountReScopeTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeDemoSettingsRepository() = mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) }

    // P3.2: ProfileViewModel now collects `sessionState.first()` in init(); a relaxed mockk's
    // auto-generated Flow<SessionState> never emits (null-collector trap), so it's stubbed here.
    private fun fakeSessionRepository() = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) }

    private fun track(
        routeId: String,
        accountId: String,
    ) = SavedTrack(
        routeId = routeId,
        name = "Track $routeId",
        startedByAccountId = accountId,
        startLatitude = 0.0,
        startLongitude = 0.0,
        endLatitude = 0.0,
        endLongitude = 0.0,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 0L,
        endTime = 1L,
        distance = 1_000.0,
        duration = 60_000L,
    )

    @Test
    fun `CommitAccountSwitch persists the new active account id to ActiveAccountSource`() =
        runTest {
            val activeAccountSource = FakeActiveAccountSource(seed = "ACC-001")
            val vm =
                ProfileViewModel(
                    FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
                    ThemeController(),
                    activeAccountSource,
                    fakeDemoSettingsRepository(),
                    fakeSessionRepository(),
                )
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-002"))
            advanceUntilIdle()

            assertEquals("ACC-002", vm.uiState.value.selectedAccountId)
            assertEquals(
                "ACC-002",
                activeAccountSource.activeAccountId.first(),
                "CommitAccountSwitch must write through to the persisted pointer, not just local VM state",
            )
        }

    @Test
    fun `CommitAccountSwitch flips the active flag in the Room-backed MockAccountRepository`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val mockAccountRepository = MockAccountRepository(accountDao)
            val vm =
                ProfileViewModel(
                    FakeProfileRepository(mockAccountRepository),
                    ThemeController(),
                    FakeActiveAccountSource(seed = "ACC-001"),
                    fakeDemoSettingsRepository(),
                    fakeSessionRepository(),
                )
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-003"))
            advanceUntilIdle()

            val accounts = mockAccountRepository.accounts()
            assertTrue(accounts.single { it.id == "ACC-003" }.isActive)
            assertTrue(accounts.filter { it.id != "ACC-003" }.none { it.isActive })
        }

    @Test
    fun `switching the active account re-scopes SavedTracksViewModel to that account's trips only`() =
        runTest {
            val trackDao = FakeSavedTrackDao()
            trackDao.preload(track("t1", "ACC-001"))
            trackDao.preload(track("t2", "ACC-001"))
            trackDao.preload(track("t3", "ACC-002"))

            val activeAccountSource = FakeActiveAccountSource(seed = "ACC-001")
            val savedTracksVm = SavedTracksViewModel(SavedTrackRepository(trackDao), activeAccountSource)
            advanceUntilIdle()

            assertEquals(
                setOf("t1", "t2"),
                savedTracksVm.state.value.tracks
                    .map { it.token }
                    .toSet(),
            )

            // Simulate ProfileViewModel.SwitchAccount's write-through to the shared store.
            activeAccountSource.setActiveAccountId("ACC-002")
            advanceUntilIdle()

            assertEquals(
                setOf("t3"),
                savedTracksVm.state.value.tracks
                    .map { it.token }
                    .toSet(),
            )
        }
}
