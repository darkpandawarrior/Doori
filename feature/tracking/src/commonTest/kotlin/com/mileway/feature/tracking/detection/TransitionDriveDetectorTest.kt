package com.mileway.feature.tracking.detection

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitionDriveDetectorTest {
    @Test
    fun vehicleEnterStartsOnceAndIdleExitDoesNotStart() {
        val detector = TransitionDriveDetector()
        assertFalse(detector.accept(DriveTransition.VEHICLE_EXIT, 1))
        assertTrue(detector.accept(DriveTransition.VEHICLE_ENTER, 2))
        assertFalse(detector.accept(DriveTransition.VEHICLE_ENTER, 3))
        assertFalse(detector.state.armed)
    }

    @Test
    fun departureRequiresVehicleAndSurvivesProcessRecreationInEitherOrder() {
        for (first in listOf(DriveTransition.DEPARTURE, DriveTransition.VEHICLE_ENTER)) {
            val detector = TransitionDriveDetector(DriveTransitionState(requiresDeparture = true))
            assertFalse(detector.accept(first, 1))
            val restored = TransitionDriveDetector(Json.decodeFromString<DriveTransitionState>(Json.encodeToString(detector.state)))
            val second = if (first == DriveTransition.DEPARTURE) DriveTransition.VEHICLE_ENTER else DriveTransition.DEPARTURE
            assertTrue(restored.accept(second, 2))
            assertFalse(restored.accept(second, 3))
        }
    }

    @Test
    fun exitCancelsMotionAndOldQueuedEventsAreIgnored() {
        val detector = TransitionDriveDetector(DriveTransitionState(requiresDeparture = true, armedAtNanos = 10))
        assertFalse(detector.accept(DriveTransition.VEHICLE_ENTER, 9))
        assertFalse(detector.accept(DriveTransition.VEHICLE_ENTER, 11))
        assertFalse(detector.accept(DriveTransition.VEHICLE_EXIT, 12))
        assertFalse(detector.accept(DriveTransition.VEHICLE_ENTER, 11))
        assertFalse(detector.accept(DriveTransition.DEPARTURE, 13))
        assertTrue(detector.accept(DriveTransition.VEHICLE_ENTER, 14))
    }

    @Test
    fun recordingTimestampsBeginAtMotionNotAtArm() {
        val drive = PendingDrive("trip", "Journey", "car", 7.0, "employee", "account", "tenant")
        val track = Json.decodeFromString<PendingDrive>(Json.encodeToString(drive)).recordingAt(50_000)
        assertEquals(50_000L, track.startTime)
        assertEquals(50_000L, track.startedAtTimestamp)
        assertEquals("car", track.selectedVehicleType)
        assertEquals("account", track.startedByAccountEmail)
    }

    @Test
    fun slcWakeRequiresAlwaysFreshAccurateDrivingSpeedFix() {
        assertTrue(SignificantDriveFixPolicy.mayWake(true, 2.0, 20.0, 10.0))
        assertFalse(SignificantDriveFixPolicy.mayWake(false, 2.0, 20.0, 10.0))
        assertFalse(SignificantDriveFixPolicy.mayWake(true, 31.0, 20.0, 10.0))
        assertFalse(SignificantDriveFixPolicy.mayWake(true, 2.0, -1.0, 10.0))
        assertFalse(SignificantDriveFixPolicy.mayWake(true, 2.0, 200.0, 10.0))
        assertFalse(SignificantDriveFixPolicy.mayWake(true, 2.0, 20.0, 1.0))
    }

    @Test
    fun alwaysUpgradeIsForegroundOnlyAndSlcCannotObtainAlways() {
        assertTrue(IosDriveWakePolicy.mayRequestAlways(isForeground = true, hasWhenInUse = true))
        assertFalse(IosDriveWakePolicy.mayRequestAlways(isForeground = false, hasWhenInUse = true))
        assertFalse(IosDriveWakePolicy.mayRequestAlways(isForeground = true, hasWhenInUse = false))
        assertFalse(IosDriveWakePolicy.mayMonitorSignificantChanges(hasAlways = false))
        assertTrue(IosDriveWakePolicy.mayMonitorSignificantChanges(hasAlways = true))
    }
}
