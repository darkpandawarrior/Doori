package com.mileway.feature.tracking

import com.mileway.feature.tracking.manager.IosTrackingController
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * P-C.2: Swift -> KMP bridge for a CoreLocation-triggered background relaunch, exported in the
 * Mileway framework. Mirrors [DeepLinkActionBridge]'s `KoinPlatform.getKoin()` resolution pattern.
 *
 * `AppDelegate.swift` calls `TrackingRelaunchBridge.shared.onSystemRelaunch()` from
 * `application(_:didFinishLaunchingWithOptions:)` when `launchOptions[.location]` is present — the OS
 * relaunched a terminated app because CoreLocation's significant-change monitoring saw movement (see
 * `IosLocationTracker.startMonitoringSignificantLocationChanges`). This marks the pending relaunch on
 * the controller (so [IosTrackingController.start] records it via `SystemRecoveryDetector`) and, if a
 * track was left active in [CurrentTrackRepository] (DataStore-backed, survives process death),
 * resumes it so distance accumulation continues instead of silently stopping.
 */
object TrackingRelaunchBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onSystemRelaunch() {
        val controller = KoinPlatform.getKoin().getOrNull<TrackingController>() as? IosTrackingController ?: return
        controller.markSystemRelaunch()
        val currentTrackRepo = KoinPlatform.getKoin().getOrNull<CurrentTrackRepository>() ?: return
        scope.launch {
            val current = currentTrackRepo.getCurrentTrackDataRawAsync().getOrNull() ?: return@launch
            if (current.isTracking) controller.start(current.token)
        }
    }
}
