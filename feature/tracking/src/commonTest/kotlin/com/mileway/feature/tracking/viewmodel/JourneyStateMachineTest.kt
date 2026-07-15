package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.feature.tracking.ui.sheets.JourneyGuideStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * C4: table-driven coverage for the tracking flow's explicit state machine — [JourneyStep]
 * ([TrackMilesUiState.journeyProgress]) and [TrackMilesPrimaryAction]
 * ([TrackMilesUiState.primaryAction]) — plus [OdometerState]'s reconciliation math.
 *
 * These are pure state → derived-value checks (no ViewModel needed); [buildVm]-based behavioural
 * tests already live in [TrackMilesViewModelTest].
 */
class JourneyStateMachineTest {
    private val vehicle = ApprovedVehicle(vehicleKey = "car", vehicleName = "Car", vehiclePricing = 10.0)

    // ── JourneyStep / primaryAction table ────────────────────────────────────────

    @Test
    fun `idle with no permissions resolves PERMISSIONS then ResolvePermissions`() {
        val state = TrackMilesUiState(permissionsSatisfied = false)
        assertEquals(JourneyStep.PERMISSIONS, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.ResolvePermissions, state.primaryAction)
    }

    @Test
    fun `idle with no vehicle resolves VEHICLE then SelectVehicle`() {
        val state = TrackMilesUiState()
        assertEquals(JourneyStep.VEHICLE, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.SelectVehicle, state.primaryAction)
    }

    @Test
    fun `vehicle picked but mandatory odometer missing resolves START_ODOMETER then CaptureStartOdometer`() {
        val state =
            TrackMilesUiState(
                selectedVehicle = vehicle,
                config = TrackMilesPluginConfig(isOdometerMandatory = true),
            )
        assertEquals(JourneyStep.START_ODOMETER, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.CaptureStartOdometer, state.primaryAction)
    }

    @Test
    fun `vehicle and start odometer both present resolves READY_TO_START then StartTracking`() {
        val state =
            TrackMilesUiState(
                selectedVehicle = vehicle,
                config = TrackMilesPluginConfig(isOdometerMandatory = true),
                odometer = OdometerState(startReading = 45_000),
            )
        assertEquals(JourneyStep.READY_TO_START, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.StartTracking, state.primaryAction)
    }

    @Test
    fun `odometer not mandatory skips START_ODOMETER straight to READY_TO_START`() {
        val state = TrackMilesUiState(selectedVehicle = vehicle)
        assertEquals(JourneyStep.READY_TO_START, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.StartTracking, state.primaryAction)
    }

    @Test
    fun `TRACKING phase resolves TRACKING then StopTracking, regardless of pause`() {
        val tracking = TrackMilesUiState(phase = TrackMilesPhase.TRACKING, selectedVehicle = vehicle)
        assertEquals(JourneyStep.TRACKING, tracking.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.StopTracking, tracking.primaryAction)

        val paused = TrackMilesUiState(phase = TrackMilesPhase.PAUSED, selectedVehicle = vehicle)
        assertEquals(JourneyStep.TRACKING, paused.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.StopTracking, paused.primaryAction)
    }

    @Test
    fun `STOPPED with no end odometer resolves READY_TO_SUBMIT then CaptureEndOdometer`() {
        val state =
            TrackMilesUiState(
                phase = TrackMilesPhase.STOPPED,
                selectedVehicle = vehicle,
                odometer = OdometerState(startReading = 45_000),
            )
        assertEquals(JourneyStep.READY_TO_SUBMIT, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.CaptureEndOdometer, state.primaryAction)
    }

    @Test
    fun `STOPPED with end odometer captured resolves SubmitTrack`() {
        val state =
            TrackMilesUiState(
                phase = TrackMilesPhase.STOPPED,
                selectedVehicle = vehicle,
                odometer = OdometerState(startReading = 45_000, endReading = 45_010),
            )
        assertEquals(JourneyStep.READY_TO_SUBMIT, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.SubmitTrack, state.primaryAction)
    }

    @Test
    fun `SUBMITTED resolves READY_TO_SUBMIT then SubmitTrack`() {
        val state = TrackMilesUiState(phase = TrackMilesPhase.SUBMITTED, selectedVehicle = vehicle)
        assertEquals(JourneyStep.READY_TO_SUBMIT, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.SubmitTrack, state.primaryAction)
    }

    // ── Existing JourneyGuideStep mapping must still hold (unrelated, narrower stepper) ──

    @Test
    fun `old journeyStep JourneyGuideStep mapping is unchanged`() {
        assertEquals(JourneyGuideStep.VEHICLE, TrackMilesUiState().journeyStep)
        assertEquals(JourneyGuideStep.TRACKING, TrackMilesUiState(selectedVehicle = vehicle).journeyStep)
    }

    // ── OdometerState ─────────────────────────────────────────────────────────────

    @Test
    fun `OdometerState computedDistance is null until both readings are captured`() {
        assertEquals(null, OdometerState().computedDistance)
        assertEquals(null, OdometerState(startReading = 45_000).computedDistance)
    }

    @Test
    fun `OdometerState computedDistance is the delta once both readings are captured`() {
        val state = OdometerState(startReading = 45_000, endReading = 45_123)
        assertEquals(123, state.computedDistance)
        assertNull(state.validationError)
    }

    @Test
    fun `OdometerState flags a validation error when the end reading precedes the start`() {
        val state = OdometerState(startReading = 45_000, endReading = 44_999)
        assertEquals(null, state.computedDistance)
        assertEquals("End odometer reading is before the start reading", state.validationError)
    }

    // ── ViewModel wiring for CaptureEndOdometer ───────────────────────────────────

    @Test
    fun `CaptureEndOdometer action stores a deterministic end reading`() {
        val vm = TrackMilesViewModelTestHarness.build()
        vm.onAction(TrackMilesAction.CaptureStartOdometer)
        val afterStart = vm.uiState.value.odometer.startReading
        requireNotNull(afterStart)

        vm.onAction(TrackMilesAction.CaptureEndOdometer)
        val odometer = vm.uiState.value.odometer
        val endReading = requireNotNull(odometer.endReading)
        assertEquals(true, endReading >= afterStart)
    }
}
