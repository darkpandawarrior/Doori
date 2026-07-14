package com.mileway.feature.tracking.viewmodel

import com.siddharth.kmp.common.UiText
import com.mileway.core.data.model.network.ApprovedVehicle

/**
 * MVI contract for the Track Miles flow (A.5a). Lives in commonMain; the ViewModel that handles
 * it stays in androidMain because it binds the foreground tracking service via `IBinder`.
 */
sealed interface TrackMilesAction {
    // Sheet orchestration
    data object OpenJourneyGuide : TrackMilesAction

    data object DismissSheet : TrackMilesAction

    data object OpenVehiclePicker : TrackMilesAction

    data class SetVehicleQuery(val query: String) : TrackMilesAction

    data class PickVehicle(val key: String) : TrackMilesAction

    data class SelectVehicle(val vehicle: ApprovedVehicle) : TrackMilesAction

    data object CaptureStartOdometer : TrackMilesAction

    data class ToggleDraft(val enabled: Boolean) : TrackMilesAction

    data object OpenVendorPicker : TrackMilesAction

    data class SetVendorQuery(val query: String) : TrackMilesAction

    data class PickVendor(val id: String) : TrackMilesAction

    // Pause / resume sheets
    data object OpenPauseSheet : TrackMilesAction

    data class SetPauseReason(val reason: String?) : TrackMilesAction

    data class SetPauseCustomReason(val text: String) : TrackMilesAction

    data class ConfirmPause(val reason: String) : TrackMilesAction

    data object OpenResumeSheet : TrackMilesAction

    data class SetResumeNotes(val notes: String) : TrackMilesAction

    data object ConfirmResume : TrackMilesAction

    // Tracking lifecycle
    data object RequestStartTracking : TrackMilesAction

    data object AcceptConsentAndStart : TrackMilesAction

    data object StopTracking : TrackMilesAction

    data object DiscardTracking : TrackMilesAction

    // Gauge
    data object ToggleGaugeMode : TrackMilesAction

    // P-C.5: session-restore sheet outcomes.
    data object RecoveryResume : TrackMilesAction

    data object RecoverySaveFinish : TrackMilesAction

    data object RecoveryDiscard : TrackMilesAction

    // P3.5: cold-start ownership-mismatch dialog outcomes ("Resume session for {other persona}?").
    data object StrangerSessionResume : TrackMilesAction

    data object StrangerSessionDismiss : TrackMilesAction

    // Wave-4 §2.1: multi-session restore sheet — resume a specific session picked from the list.
    data class MultiSessionResume(val routeId: String) : TrackMilesAction
}

sealed interface TrackMilesEffect {
    data class ShowToast(val message: UiText) : TrackMilesEffect
}
