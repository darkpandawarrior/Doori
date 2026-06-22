package com.miletracker.feature.tracking.manager

import com.miletracker.core.data.model.display.TrackingState
import com.miletracker.core.platform.LocationTracker
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.service.SystemRecoveryDetector
import com.miletracker.feature.tracking.service.TrackingSnapshot
import com.miletracker.feature.tracking.service.TrackingStatePublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * P-B.1: iOS implementation of [TrackingController].
 *
 * Drives [LocationTracker] (CoreLocation via [IosLocationTracker]) for location fix delivery and
 * publishes live state through [TrackingStatePublisher]. Full pipeline integration (P-B.3) will wire
 * the fixes into [com.miletracker.feature.tracking.service.location.LocationProcessor] and enable
 * Always authorization + significant-change relaunch for L2/L3 lifecycle recovery.
 *
 * iOS-specific details (Info.plist `location` bg mode, NSLocationAlwaysUsageDescription, background
 * session limits) are documented in .ralph/PROGRESS.md.
 */
class IosTrackingController(
    private val locationTracker: LocationTracker,
    private val statePublisher: TrackingStatePublisher,
    private val trackRepository: SavedTrackRepository? = null,
) : TrackingController {
    private var activeToken: String? = null
    private var scope: CoroutineScope? = null

    // P-C.2: set to true when the iOS app was relaunched by CLLocationManager
    // (significant-change monitoring) after an OS-kill. Swift/AppDelegate must call
    // markSystemRelaunch() before start() if it detects a launchOptions[locationKey].
    private var pendingSystemRelaunch = false

    fun markSystemRelaunch() {
        pendingSystemRelaunch = true
    }

    override fun start(token: String) {
        if (activeToken != null) return
        val wasSystemRelaunch = pendingSystemRelaunch
        pendingSystemRelaunch = false
        activeToken = token
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = sessionScope
        locationTracker.start()
        statePublisher.update {
            TrackingSnapshot(state = TrackingState.LIVE_TRACKING, token = token)
        }
        // P-C.2: if the app was relaunched by the OS (significant-change), mark the flag.
        if (wasSystemRelaunch && trackRepository != null) {
            sessionScope.launch {
                SystemRecoveryDetector(trackRepository).handleIfSystemRecovery(token, true)
            }
        }
        // TODO(ios/P-B.3): wire fixes into LocationProcessor + CounterReconcilePolicy here.
        sessionScope.launch {
            locationTracker.updates.collect { _ ->
                statePublisher.update { snap ->
                    snap.copy(
                        // Placeholder: P-B.3 accumulates real distance and speed via LocationProcessor.
                        totalPoints = snap.totalPoints + 1,
                    )
                }
            }
        }
    }

    override fun pause(token: String) {
        if (activeToken != token) return
        locationTracker.stop()
        statePublisher.update { it.copy(state = TrackingState.PAUSED) }
    }

    override fun resume(token: String) {
        if (activeToken != token) return
        locationTracker.start()
        statePublisher.update { it.copy(state = TrackingState.LIVE_TRACKING) }
    }

    override fun stop(token: String) {
        if (activeToken != token) return
        activeToken = null
        scope?.cancel()
        scope = null
        locationTracker.stop()
        statePublisher.update { TrackingSnapshot(state = TrackingState.COMPLETED) }
    }
}
