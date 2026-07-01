package com.mileway.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * iOS location via CoreLocation, counterpart to Android's FusedLocationProvider.
 *
 * P-B.3 upgrades:
 * - requestAlwaysAuthorization() so the OS keeps delivering fixes when the app is backgrounded.
 * - allowsBackgroundLocationUpdates = true: required for continuous background tracking (the `location`
 *   background mode must also be listed in Info.plist — already present since V12).
 * - pausesLocationUpdatesAutomatically = false: prevents CoreLocation from silently halting delivery
 *   when the device is stationary, which would produce false zero-distance periods.
 * - startMonitoringSignificantLocationChanges(): lets the OS relaunch a terminated/killed app when the
 *   device moves ~500 m; satisfies L2 (OS-kill relaunch) and L3 (reboot) in the lifecycle matrix.
 *
 * Info.plist requirements (already present):
 *   UIBackgroundModes: location
 *   NSLocationAlwaysAndWhenInUseUsageDescription: "Mileway needs Always location to track your
 *     mileage even when the app is in the background."
 *   NSLocationWhenInUseUsageDescription: (same string, for the initial prompt)
 */
class IosLocationTracker : LocationTracker {
    private val _updates = MutableSharedFlow<GeoPoint>(replay = 1)
    override val updates: Flow<GeoPoint> = _updates.asSharedFlow()

    private val manager = CLLocationManager()
    private var lastKnown: GeoPoint? = null

    private val locationDelegate =
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>,
            ) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                val point = location.toGeoPoint()
                lastKnown = point
                _updates.tryEmit(point)
            }
        }

    init {
        manager.delegate = locationDelegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        // P-B.3: continuous background delivery + suppress OS auto-pause.
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        // P-B.3: relaunch hook for L2 (OS-kill) + L3 (reboot) via significant-change monitoring.
        manager.startMonitoringSignificantLocationChanges()
    }

    override suspend fun current(): GeoPoint? = lastKnown ?: manager.location?.toGeoPoint()

    override fun start() {
        // P-B.3: request Always auth so the manager continues delivering fixes in the background.
        manager.requestAlwaysAuthorization()
        manager.startUpdatingLocation()
    }

    override fun stop() {
        manager.stopUpdatingLocation()
        // Significant-change monitoring stays active (it's the relaunch hook — don't stop it).
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun CLLocation.toGeoPoint(): GeoPoint =
        coordinate.useContents {
            GeoPoint(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = horizontalAccuracy.toFloat(),
                timestampMillis = (timestamp.timeIntervalSince1970 * 1000.0).toLong(),
            )
        }
}
