package com.mileway.feature.tracking.detection

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.viewmodel.FakeSavedTrackDao
import com.mileway.feature.tracking.viewmodel.TrackMilesPhase
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModelTestHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DriveStartIntegrationTest {
    @Test
    fun armAndCancelDoNotStartRecordingAndManualStartDisarmsWait() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val controller = RecordingController()
                val source = FakeDriveSource()
                val vm = TrackMilesViewModelTestHarness.build(controller = controller, driveStartSource = source)
                vm.selectVehicle(ApprovedVehicle(vehicleKey = "car", vehiclePricing = 7.0))
                vm.waitForDrive()
                assertTrue(vm.uiState.value.waitingForDrive)
                assertEquals(TrackMilesPhase.IDLE, vm.uiState.value.phase)
                assertTrue(controller.started.isEmpty())
                assertNotNull(source.pending)
                vm.cancelDriveWait()
                assertFalse(vm.uiState.value.waitingForDrive)
                vm.waitForDrive()
                vm.startTracking()
                assertFalse(source.waiting.value)
                assertEquals(1, controller.started.size)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun motionCreatesTripAtRecordingTimeAndRefusedServiceLeavesNoGhostTrip() =
        runTest {
            val dao = FakeSavedTrackDao(emptyList())
            val tracks = SavedTrackRepository(dao)
            val controller = RecordingController()
            val recorder = DetectedDriveRecorder(tracks, controller)
            val drive = PendingDrive("trip", "Journey", "car", 7.0, "employee", "account", "tenant")
            assertNull(tracks.getActiveTrack())
            recorder.start(drive)
            assertEquals(listOf("trip"), controller.started)
            assertTrue(assertNotNull(tracks.getByRouteId("trip")).startTime > 0)
            tracks.delete("trip")
            controller.refuse = true
            val failure = runCatching { recorder.start(drive) }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
            assertNull(tracks.getActiveTrack())
        }

    private class RecordingController : TrackingController {
        val started = mutableListOf<String>()
        var refuse = false

        override fun start(token: String) {
            check(!refuse)
            started += token
        }

        override fun pause(token: String) = Unit

        override fun resume(token: String) = Unit

        override fun stop(token: String) = Unit
    }

    private class FakeDriveSource : DriveStartSource {
        override val waiting = MutableStateFlow(false)
        var pending: PendingDrive? = null

        override suspend fun arm(
            drive: PendingDrive,
            departure: DriveDeparture?,
        ): Boolean {
            pending = drive
            waiting.value = true
            return true
        }

        override suspend fun disarm() {
            pending = null
            waiting.value = false
        }
    }
}
