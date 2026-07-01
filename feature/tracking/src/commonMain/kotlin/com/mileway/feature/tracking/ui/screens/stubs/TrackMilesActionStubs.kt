package com.mileway.feature.tracking.ui.screens.stubs

/**
 * Action model for the Track Miles screen family (MVI user intents).
 *
 * Kept as a flat sealed hierarchy in a single file so the complete set of
 * user intents the tracking surface can emit is visible at a glance. Screens
 * dispatch these; the ViewModel is the single reducer.
 */
sealed interface TrackMilesAction

// ── Tracking lifecycle ─────────────────────────────────────────────────────────
object StartTracking : TrackMilesAction

object StopTracking : TrackMilesAction

object PauseTracking : TrackMilesAction

object ResumeTracking : TrackMilesAction

object DiscardJourney : TrackMilesAction

object HideConfirmDialog : TrackMilesAction

object DismissError : TrackMilesAction

// ── Navigation ────────────────────────────────────────────────────────────────
object OpenLogs : TrackMilesAction

object OpenMap : TrackMilesAction

object OpenSettings : TrackMilesAction

object OpenTrackSubmission : TrackMilesAction

object OpenManualCheckIn : TrackMilesAction

object OpenGeoCheckIn : TrackMilesAction

object OpenSavedTracks : TrackMilesAction

object OpenLoadingScreen : TrackMilesAction

object OpenDataStoreScreen : TrackMilesAction

object CheckInHistory : TrackMilesAction

object OutletsList : TrackMilesAction

data class OpenTrackDetail(val token: String) : TrackMilesAction

data class OpenTrackSubmissionWithContext(
    val routeId: String,
    val isDraft: Boolean = false,
    val isDiscarded: Boolean = false,
) : TrackMilesAction

// ── Vehicle selection ─────────────────────────────────────────────────────────
data class SelectVehicle(val vehicleType: Any?) : TrackMilesAction
