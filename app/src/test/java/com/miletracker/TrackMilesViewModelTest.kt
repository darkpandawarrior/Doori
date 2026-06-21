package com.miletracker

import app.cash.turbine.test
import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.network.api.MileTrackerNetworkApi
import com.miletracker.feature.tracking.manager.LocationTrackingController
import com.miletracker.feature.tracking.manager.TrackingConfigManager
import com.miletracker.feature.tracking.repository.CurrentTrackRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.repository.VehiclePricingRepository
import com.miletracker.feature.tracking.service.TrackingStatePublisher
import com.miletracker.feature.tracking.ui.sheets.JourneyGuideStep
import com.miletracker.feature.tracking.viewmodel.TrackMilesPhase
import com.miletracker.feature.tracking.viewmodel.TrackMilesUiState
import com.miletracker.feature.tracking.viewmodel.TrackMilesViewModel
import com.miletracker.stub.DemoConfigManager
import com.miletracker.stub.FakeTrackingNetworkApi
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavioural tests for [TrackMilesViewModel], the reducer behind the live-tracking screen.
 *
 * Test-double strategy (fakes at the boundary, mocks only for side effects):
 *  - [FakeSavedTrackDao], a real in-memory implementation of the Room DAO slice the
 *    ViewModel exercises, so the genuine [SavedTrackRepository] runs unmodified and
 *    Flow emissions behave exactly like Room's invalidation tracker (state-backed).
 *  - [FakeTrackingNetworkApi] / [DemoConfigManager], the same stub network layer the
 *    app ships with for offline-first dev; tests and the running app share one fake.
 *  - [LocationTrackingController], the only mock. It is a fire-and-forget façade over
 *    `startForegroundService`, so the contract IS the interaction: verify, don't fake.
 *
 * The foreground service is not run here; [FakeSavedTrackDao.serviceWrites] simulates the
 * service's live `saved_tracks` writes, which is the real production contract between the
 * service and the ViewModel (they communicate only through the database).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrackMilesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeSavedTrackDao()
    private val trackingController = mockk<LocationTrackingController>(relaxUnitFun = true)

    private val trackingPublisher = TrackingStatePublisher()

    private fun viewModel(api: MileTrackerNetworkApi = FakeTrackingNetworkApi()) =
        TrackMilesViewModel(
            configManager = TrackingConfigManager(DemoConfigManager()),
            vehicleRepo = VehiclePricingRepository(api),
            trackRepo = SavedTrackRepository(dao),
            trackingController = trackingController,
            currentTrackRepo = CurrentTrackRepository(mockk(relaxed = true)),
            locationRepo = LocationRepository(mockk(relaxed = true)),
            trackingServiceApi = trackingPublisher,
        )

    // ── C.3: live service telemetry feed ─────────────────────────────────────

    @Test
    fun `surfaces adaptive GPS cadence and battery from the tracking service feed`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(0L, vm.uiState.value.gpsIntervalMs)

        trackingPublisher.update { it.copy(currentIntervalMs = 8_000L, batteryPct = 42, isCharging = true) }
        advanceUntilIdle()

        assertEquals(8_000L, vm.uiState.value.gpsIntervalMs)
        assertEquals(42, vm.uiState.value.batteryPct)
        assertTrue(vm.uiState.value.isCharging)
    }

    // ── Initialisation ───────────────────────────────────────────────────────

    @Test
    fun `init loads vehicle policy and auto-selects the first vehicle`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.vehicles.isNotEmpty())
        assertEquals("fourWheelerPetrol", state.selectedVehicle?.vehicleKey)
        assertTrue(state.config.isTrackMilesEnabled)
    }

    // ── G4: consolidated, VM-owned start-flow step ───────────────────────────

    @Test
    fun `journeyStep is VM-owned and derives from vehicle selection`() = runTest {
        // No vehicle yet → the stepper sits on the VEHICLE step.
        assertEquals(JourneyGuideStep.VEHICLE, TrackMilesUiState().journeyStep)

        // After init auto-selects a vehicle, the VM-owned step advances to TRACKING
        // (the screen no longer derives this inline — G4 consolidation).
        val vm = viewModel()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state.selectedVehicle)
        assertEquals(JourneyGuideStep.TRACKING, state.journeyStep)
    }

    @Test
    fun `vehicle policy failure degrades gracefully instead of crashing the screen`() = runTest {
        val failingApi = object : MileTrackerNetworkApi by FakeTrackingNetworkApi() {
            override suspend fun vehicles(trackMiles: Boolean) =
                throw IOException("airplane mode")
        }

        val vm = viewModel(api = failingApi)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.vehicles.isEmpty())
        assertNull(state.selectedVehicle)
        // Config still loads, the screen renders, it just can't start a paid journey.
        assertTrue(state.config.isTrackMilesEnabled)
    }

    @Test
    fun `init restores an in-flight track after process death`() = runTest {
        dao.preload(
            track(routeId = "route-restored", distanceMeters = 4_200.0, startTime = 1_000L)
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TrackMilesPhase.TRACKING, state.phase)
        assertEquals("route-restored", state.currentRouteId)
        assertEquals(4.2, state.distanceKm, 1e-9)
        assertEquals(1_000L, state.startTime)
    }

    // ── Tracking lifecycle ───────────────────────────────────────────────────

    @Test
    fun `startTracking persists an active track and starts the foreground service`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startTracking()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TrackMilesPhase.TRACKING, state.phase)
        val routeId = assertNotNull(state.currentRouteId)

        // The track must be durably persisted BEFORE the service starts writing to it.
        val persisted = assertNotNull(dao.getSavedTrackById(routeId))
        assertEquals("fourWheelerPetrol", persisted.selectedVehicleType)
        verify(exactly = 1) { trackingController.start(routeId) }
    }

    @Test
    fun `live service writes stream distance and reimbursement into ui state`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startTracking()
        advanceUntilIdle()
        val routeId = vm.uiState.value.currentRouteId!!

        vm.uiState.map { it.distanceKm to it.reimbursableAmount }.test {
            assertEquals(0.0 to 0.0, awaitItem())

            dao.serviceWrites(routeId, distanceMeters = 12_500.0, durationMs = 900_000L)
            advanceUntilIdle()

            // 12.5 km × ₹10/km (fourWheelerPetrol demo pricing) = ₹125
            assertEquals(12.5 to 125.0, awaitItem())
            assertEquals(900_000L, vm.uiState.value.durationMs)
        }
    }

    @Test
    fun `pause and resume forward to the service and flip the phase`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startTracking()
        advanceUntilIdle()
        val routeId = vm.uiState.value.currentRouteId!!

        vm.pauseTracking()
        assertEquals(TrackMilesPhase.PAUSED, vm.uiState.value.phase)
        verify(exactly = 1) { trackingController.pause(routeId) }

        vm.resumeTracking()
        assertEquals(TrackMilesPhase.TRACKING, vm.uiState.value.phase)
        verify(exactly = 1) { trackingController.resume(routeId) }
    }

    @Test
    fun `stopTracking stops the service and freezes the live stream`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startTracking()
        advanceUntilIdle()
        val routeId = vm.uiState.value.currentRouteId!!

        dao.serviceWrites(routeId, distanceMeters = 8_000.0, durationMs = 600_000L)
        advanceUntilIdle()
        assertEquals(8.0, vm.uiState.value.distanceKm, 1e-9)

        vm.stopTracking()
        verify(exactly = 1) { trackingController.stop(routeId) }
        assertEquals(TrackMilesPhase.STOPPED, vm.uiState.value.phase)

        // A straggler write from the (now stopping) service must not mutate the
        // stopped summary the user is looking at.
        dao.serviceWrites(routeId, distanceMeters = 99_000.0, durationMs = 999_999L)
        advanceUntilIdle()
        assertEquals(8.0, vm.uiState.value.distanceKm, 1e-9)
    }

    @Test
    fun `discardTracking resets the session but keeps config and vehicle policy`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startTracking()
        advanceUntilIdle()
        val routeId = vm.uiState.value.currentRouteId!!

        vm.discardTracking()
        advanceUntilIdle()

        val state = vm.uiState.value
        verify(exactly = 1) { trackingController.stop(routeId) }
        assertEquals(TrackMilesPhase.IDLE, state.phase)
        assertNull(state.currentRouteId)
        assertEquals(0.0, state.distanceKm, 1e-9)
        // Discard ends the journey, not the screen: policy data survives the reset.
        assertTrue(state.vehicles.isNotEmpty())
        assertTrue(state.config.isTrackMilesEnabled)
    }

    // ── Test fixtures ────────────────────────────────────────────────────────

    private fun track(
        routeId: String,
        distanceMeters: Double,
        startTime: Long
    ) = SavedTrack(
        routeId = routeId,
        name = "Test journey",
        startLatitude = 18.5204, startLongitude = 73.8567,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = startTime, endTime = -1L,
        distance = distanceMeters, duration = 0L,
        selectedVehicleType = "fourWheelerPetrol",
        vehiclePricing = 10.0,
        createdAt = startTime, startedAtTimestamp = startTime,
        startedByEmployeeCode = "EMP001"
    )
}

/**
 * In-memory [SavedTrackDao] backed by a [MutableStateFlow], so `observeTrackById`
 * re-emits on every write, the same observable behaviour Room's invalidation
 * tracker provides over the real database.
 *
 * The DAO interface is wide; only the slice [SavedTrackRepository] touches is
 * implemented for real. Everything else delegates to a strict MockK stub, so an
 * unexpected DAO call fails the test loudly instead of silently returning defaults.
 */
class FakeSavedTrackDao(
    unusedSurface: SavedTrackDao = mockk(relaxed = true)
) : SavedTrackDao by unusedSurface {

    private val tracks = MutableStateFlow<Map<String, SavedTrack>>(emptyMap())

    fun preload(track: SavedTrack) {
        tracks.update { it + (track.routeId to track) }
    }

    /** Removes a track so observers see the updated list, useful for selection-pruning tests. */
    fun removeTrack(routeId: String) {
        tracks.update { it - routeId }
    }

    /** Simulates the foreground service's periodic live write to `saved_tracks`. */
    fun serviceWrites(routeId: String, distanceMeters: Double, durationMs: Long) {
        tracks.update { current ->
            val track = requireNotNull(current[routeId]) { "no track $routeId" }
            current + (routeId to track.copy(distance = distanceMeters, duration = durationMs))
        }
    }

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) = preload(savedTrack)

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? =
        tracks.value[routeId]

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> =
        tracks.map { it[routeId] }

    override suspend fun getActiveTrack(): SavedTrack? =
        tracks.value.values.firstOrNull { !it.isCompleted }

    override fun getCompletedTracks(): Flow<List<SavedTrack>> =
        tracks.map { it.values.filter { t -> t.isCompleted }.toList() }

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> =
        tracks.map { it.values.toList() }

    override suspend fun count(): Long = tracks.value.size.toLong()
}
