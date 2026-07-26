package com.mileway.feature.tracking.viewmodel

import com.mileway.feature.tracking.checkin.CheckInValidator
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [CheckInViewModel]'s manual + geo check-in state machine.
 *
 * Reuses [FakeLocationDao]/[FakeCurrentTrackDataSource] from `TrackMilesViewModelTestHarness.kt` and
 * [EventLogDao] from `HardwareEventsViewModelTest.kt` rather than declaring local copies — those
 * three interfaces total ~60 members, and a same-named redeclaration in this package is a compile
 * error, which is exactly what broke an earlier attempt at this file.
 *
 * [FakeCurrentTrackDataSource] yields a blank token, so these cover the no-active-session branches
 * and the pure geo-validation paths. Exercising a live session would need a settable token on that
 * shared object; deliberately out of scope here rather than mutating a fake other tests depend on.
 */
class CheckInViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(locations: List<CheckInValidator.CheckInLocation> = SITES) =
        CheckInViewModel(
            locationRepo = LocationRepository(FakeLocationDao()),
            hardwareEventRepo = HardwareEventRepository(EventLogDao()),
            currentTrackRepository = CurrentTrackRepository(FakeCurrentTrackDataSource),
            geoCheckInLocations = locations,
        )

    @Test
    fun `sheets open and close`() =
        runTest {
            val vm = viewModel()

            vm.onAction(CheckInAction.OpenManualCheckIn)
            assertTrue(vm.state.value.showManualCheckInSheet)
            vm.onAction(CheckInAction.DismissManualCheckIn)
            assertTrue(!vm.state.value.showManualCheckInSheet)

            vm.onAction(CheckInAction.OpenGeoCheckIn)
            assertTrue(vm.state.value.showGeoCheckInSheet)
            vm.onAction(CheckInAction.DismissGeoCheckIn)
            assertTrue(!vm.state.value.showGeoCheckInSheet)
        }

    @Test
    fun `manual reason is captured`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CheckInAction.UpdateManualReason("Delivered at gate 3"))
            assertEquals("Delivered at gate 3", vm.state.value.manualReason)
        }

    @Test
    fun `manual submit without an active session surfaces an error instead of failing silently`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CheckInAction.UpdateManualReason("No session"))

            vm.onAction(CheckInAction.SubmitManualCheckIn)

            val state = vm.state.value
            assertEquals("No active tracking session found.", state.error)
            assertTrue(!state.isSubmitting, "isSubmitting must be cleared or the sheet spins forever")
        }

    @Test
    fun `geo check-in with no configured locations reports that, and closes the sheet`() =
        runTest {
            val vm = viewModel(locations = emptyList())
            vm.onAction(CheckInAction.OpenGeoCheckIn)

            vm.onAction(CheckInAction.ValidateAndGeoCheckIn(lat = 18.52, lng = 73.85))

            val state = vm.state.value
            assertEquals("No check-in locations configured.", state.error)
            assertTrue(!state.showGeoCheckInSheet)
        }

    @Test
    fun `a point outside every radius raises the warning rather than checking in`() =
        runTest {
            val vm = viewModel()

            // Far from the single configured site — validation must NOT silently persist.
            vm.onAction(CheckInAction.ValidateAndGeoCheckIn(lat = 28.61, lng = 77.20))

            val state = vm.state.value
            assertTrue(state.showRadiusWarning, "an out-of-radius point must prompt, not check in")
            assertTrue(state.radiusWarningMessage.isNotBlank())
            assertTrue(!state.checkInSuccess)
        }

    @Test
    fun `dismissing the radius warning clears it and the pending result`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CheckInAction.ValidateAndGeoCheckIn(lat = 28.61, lng = 77.20))
            assertTrue(vm.state.value.showRadiusWarning)

            vm.onAction(CheckInAction.DismissRadiusWarning)

            val state = vm.state.value
            assertTrue(!state.showRadiusWarning)
            assertEquals(null, state.pendingValidationResult)
        }

    @Test
    fun `overriding the radius warning still requires an active session`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CheckInAction.ValidateAndGeoCheckIn(lat = 28.61, lng = 77.20))

            vm.onAction(CheckInAction.ForceGeoCheckInDespiteRadius)

            // The override bypasses the RADIUS check, not the session check — worth pinning, since
            // "force" could plausibly be read as bypassing everything.
            assertEquals("No active tracking session found.", vm.state.value.error)
        }

    @Test
    fun `ClearError and AcknowledgeSuccess reset their fields`() =
        runTest {
            val vm = viewModel(locations = emptyList())
            vm.onAction(CheckInAction.ValidateAndGeoCheckIn(lat = 18.52, lng = 73.85))
            assertTrue(vm.state.value.error != null)

            vm.onAction(CheckInAction.ClearError)
            assertEquals(null, vm.state.value.error)

            vm.onAction(CheckInAction.AcknowledgeSuccess)
            val state = vm.state.value
            assertTrue(!state.checkInSuccess)
            assertEquals("", state.successMessage)
        }

    private companion object {
        val SITES =
            listOf(
                CheckInValidator.CheckInLocation(
                    id = "site-1",
                    name = "Pune Supply Centre",
                    lat = 18.5204,
                    lng = 73.8567,
                    type = "SUPPLY_CENTRE",
                    radiusMeters = 100.0,
                ),
            )
    }
}
