package com.mileway

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.PERSONA_SWITCH_PAUSE_NAME
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * PLAN_V22 P3.4 — `ProfileViewModel.CommitAccountSwitch` runs [MockAccountSessionCoordinator]
 * before flipping the active-account pointer: switching persona mid-trip pauses+persists the
 * outgoing persona's running trip (surfacing [com.mileway.feature.profile.model.ProfileUiState
 * .pausedTripNotice]) and restores the incoming persona's own paused trip if one exists;
 * switching with no active trip is silent (no notice).
 */
class PersonaSwitchPauseRestoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeDemoSettingsRepository() =
        mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) }

    // P3.2: ProfileViewModel now collects `sessionState.first()` in init(); a relaxed mockk's
    // auto-generated Flow<SessionState> never emits (null-collector trap), so it's stubbed here.
    private fun fakeSessionRepository() =
        mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) }

    private class FakeSessionTrackDao : SavedTrackDao by mockk(relaxed = true) {
        val tracks = mutableMapOf<String, SavedTrack>()

        override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int {
            tracks[savedTrack.routeId] = savedTrack
            return 1
        }

        override suspend fun getSavedTrackById(routeId: String): SavedTrack? = tracks[routeId]

        override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? =
            tracks.values.firstOrNull { it.startedByEmployeeCode == employeeCode && !it.isCompleted && !it.isDiscarded && !it.isDraft }
    }

    private class FakeLiveSession(initial: CurrentTrackData) : CurrentTrackDataSource {
        val flow = MutableStateFlow(initial)
        var clearCalls = 0
            private set

        override val currentTrackFlow: Flow<CurrentTrackData> = flow

        override val syncSessionOverrideFlow: Flow<com.mileway.core.data.session.SyncSessionOverride?> =
            MutableStateFlow(null)

        override suspend fun setSyncSessionOverride(override: com.mileway.core.data.session.SyncSessionOverride?) = Unit

        override suspend fun saveSession(data: CurrentTrackData) {
            flow.value = data
        }

        override suspend fun updateDistance(
            token: String,
            distanceMeters: Double,
            speed: Double,
            avgSpeed: Double,
        ) = Unit

        override suspend fun updateLocationCount(
            token: String,
            total: Long,
            unsynced: Long,
        ) = Unit

        override suspend fun markPaused(
            token: String,
            lat: Double,
            lng: Double,
        ) = Unit

        override suspend fun markResumed(token: String) = Unit

        override suspend fun markStopped(
            token: String,
            endLat: Double,
            endLng: Double,
        ) = Unit

        override suspend fun clearSession() {
            clearCalls++
            flow.value = CurrentTrackData.empty()
        }

        override suspend fun updateLastHardwareEvent(
            token: String,
            eventText: String,
        ) = Unit
    }

    private fun track(
        routeId: String,
        employeeCode: String,
    ) = SavedTrack(
        routeId = routeId,
        name = "Journey $routeId",
        startedByEmployeeCode = employeeCode,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 0L, endTime = -1L,
        distance = 500.0, duration = 5_000L,
    )

    private fun viewModel(
        trackDao: FakeSessionTrackDao,
        liveSession: FakeLiveSession,
        accountDao: FakeMockAccountDao,
    ): ProfileViewModel {
        val mockAccountRepository = MockAccountRepository(accountDao)
        return ProfileViewModel(
            FakeProfileRepository(mockAccountRepository),
            ThemeController(),
            FakeActiveAccountSource(seed = "ACC-001"),
            fakeDemoSettingsRepository(),
            fakeSessionRepository(),
            MockAccountSessionCoordinator(liveSession, trackDao, accountDao),
        )
    }

    @Test
    fun `switching persona mid-trip pauses the outgoing trip and surfaces the notice`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val trackDao = FakeSessionTrackDao().apply { tracks["route-1"] = track("route-1", "EMP001") }
            val liveSession = FakeLiveSession(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP001"))
            val vm = viewModel(trackDao, liveSession, accountDao)
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-002"))
            advanceUntilIdle()

            assertEquals(PERSONA_SWITCH_PAUSE_NAME, trackDao.tracks.getValue("route-1").name)
            assertEquals(1, liveSession.clearCalls)
            assertEquals("ACC-002", vm.uiState.value.pausedTripNotice)
        }

    @Test
    fun `switching persona restores the incoming persona's own paused trip`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val trackDao =
                FakeSessionTrackDao().apply {
                    tracks["route-1"] = track("route-1", "EMP001")
                    tracks["route-2"] = track("route-2", "EMP001-SBX")
                }
            val liveSession = FakeLiveSession(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP001"))
            val vm = viewModel(trackDao, liveSession, accountDao)
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-002"))
            advanceUntilIdle()

            assertEquals("route-2", liveSession.flow.value.token)
            assertEquals("EMP001-SBX", liveSession.flow.value.startedByEmployeeCode)
        }

    @Test
    fun `switching persona with no active trip is silent`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val trackDao = FakeSessionTrackDao()
            val liveSession = FakeLiveSession(CurrentTrackData.empty())
            val vm = viewModel(trackDao, liveSession, accountDao)
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-002"))
            advanceUntilIdle()

            assertEquals(0, liveSession.clearCalls)
            assertNull(vm.uiState.value.pausedTripNotice)
            assertEquals("ACC-002", vm.uiState.value.selectedAccountId)
        }

    @Test
    fun `DismissPausedTripNotice clears the notice`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val trackDao = FakeSessionTrackDao().apply { tracks["route-1"] = track("route-1", "EMP001") }
            val liveSession = FakeLiveSession(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP001"))
            val vm = viewModel(trackDao, liveSession, accountDao)
            advanceUntilIdle()

            vm.onAction(ProfileAction.CommitAccountSwitch("ACC-002"))
            advanceUntilIdle()
            assertEquals("ACC-002", vm.uiState.value.pausedTripNotice)

            vm.onAction(ProfileAction.DismissPausedTripNotice)
            advanceUntilIdle()
            assertNull(vm.uiState.value.pausedTripNotice)
        }
}
