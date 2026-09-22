package com.mileway.core.data.session

import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import kotlinx.coroutines.flow.first

/** Name stamped on a trip that was auto-paused because its owning persona switched away mid-trip. */
const val PERSONA_SWITCH_PAUSE_NAME: String = "Paused - Persona Switch"

/**
 * PLAN_V22 P3.4: today, switching persona mid-trip silently orphans the running trip — the old
 * persona's [CurrentTrackDataSource] session keeps ticking under the new persona's identity, and
 * the new persona's own paused trip (if any) is never brought back. This coordinator is the
 * pause/restore hook that fixes that, mirroring the reference app's account-logout/-login pairing
 * without the FCM/network-cancellation half (no backend/network layer exists here to cancel
 * against — see PLAN_V22 §6).
 *
 * Deliberately narrow: this only touches [CurrentTrackDataSource] (the live/foreground DataStore
 * mirror) and [SavedTrackDao] (the Room source of truth for a trip row) — it does not start, stop,
 * or otherwise reach into the foreground tracking service/`TrackingController`, which stays
 * `feature/tracking`'s own concern.
 */
class MockAccountSessionCoordinator(
    private val currentTrackDataSource: CurrentTrackDataSource,
    private val savedTrackDao: SavedTrackDao,
    private val mockAccountDao: MockAccountDao,
) {
    /** What happened when [onPersonaSwitch] ran — lets the caller decide whether to surface a sheet. */
    sealed interface Outcome {
        /** No trip was active for the outgoing persona; nothing to pause, so nothing to confirm either. */
        data object NoActiveTrip : Outcome

        /** The outgoing persona's trip was paused+persisted; [restoredRouteId] is set if the incoming persona had one of its own paused/ongoing. */
        data class Paused(
            val pausedRouteId: String,
            val restoredRouteId: String?,
        ) : Outcome
    }

    /**
     * Runs before `ActiveAccountStore`/`ProfileRepository.setActiveAccount` flip the active
     * pointer to [newAccountId] (see `ProfileViewModel.SwitchAccount`/`CommitAccountSwitch`), so a
     * caller that gets [Outcome.Paused] back can still show a confirmation sheet before finishing
     * the switch.
     */
    suspend fun onPersonaSwitch(newAccountId: String): Result<Outcome> =
        runCatching {
            val newAccount = mockAccountDao.getById(newAccountId)
            val newEmployeeCode = newAccount?.employeeCode

            val liveSession = currentTrackDataSource.currentTrackFlow.first()
            val isDifferentAccount =
                liveSession.startedByEmployeeCode.isNotBlank() &&
                    newEmployeeCode != null &&
                    liveSession.startedByEmployeeCode != newEmployeeCode

            val pausedRouteId =
                if (liveSession.isTracking && liveSession.token.isNotBlank() && isDifferentAccount) {
                    pauseOutgoingTrip(liveSession)
                } else {
                    null
                }

            if (pausedRouteId == null && !liveSession.isTracking) return@runCatching Outcome.NoActiveTrip

            val restoredRouteId = newEmployeeCode?.let { restoreIncomingTrip(it) }
            if (pausedRouteId != null) {
                Outcome.Paused(pausedRouteId, restoredRouteId)
            } else {
                Outcome.NoActiveTrip
            }
        }

    /** Persists the outgoing persona's live session to its `SavedTrack` row, renamed, then clears the live mirror. */
    private suspend fun pauseOutgoingTrip(liveSession: CurrentTrackData): String? {
        val track = savedTrackDao.getSavedTrackById(liveSession.token) ?: return null
        savedTrackDao.updateSavedTrack(track.copy(name = PERSONA_SWITCH_PAUSE_NAME))
        currentTrackDataSource.clearSession()
        return track.routeId
    }

    /** Restores the incoming persona's own paused/ongoing trip (if any) into the live mirror. */
    private suspend fun restoreIncomingTrip(newEmployeeCode: String): String? {
        val existing = savedTrackDao.getActiveTrackByAccount(newEmployeeCode) ?: return null
        currentTrackDataSource.saveSession(
            CurrentTrackData(
                token = existing.routeId,
                startLatitude = existing.startLatitude,
                startLongitude = existing.startLongitude,
                endLatitude = existing.endLatitude,
                endLongitude = existing.endLongitude,
                pausedLatitude = existing.pausedLatitude,
                pausedLongitude = existing.pausedLongitude,
                startTime = existing.startTime,
                endTime = existing.endTime,
                distance = existing.distance,
                isTracking = true,
                isPaused = true,
                selectedVehicleType = existing.selectedVehicleType,
                vehiclePricing = existing.vehiclePricing,
                service = existing.service,
                trackingActivity = existing.trackingActivity,
                wasEverPaused = true,
                startedAtTimestamp = existing.startedAtTimestamp,
                startedByEmployeeCode = existing.startedByEmployeeCode,
                startedByAccountEmail = existing.startedByAccountEmail,
                startedByTenant = existing.startedByTenant,
            ),
        )
        return existing.routeId
    }
}
