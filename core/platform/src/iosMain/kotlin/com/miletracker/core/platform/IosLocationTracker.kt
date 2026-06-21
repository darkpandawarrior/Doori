package com.miletracker.core.platform

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
 * iOS location via CoreLocation (F), the CLLocationManager counterpart to Android's FusedLocation.
 * Fixes arrive through a CLLocationManagerDelegate; each is mapped to a platform-neutral [GeoPoint] and
 * pushed onto the hot [updates] flow. Compiles + links against the simulator framework; live fixes need a
 * device with location permission (and the NSLocationWhenInUseUsageDescription Info.plist key).
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
    }

    override suspend fun current(): GeoPoint? = lastKnown ?: manager.location?.toGeoPoint()

    override fun start() {
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()
    }

    override fun stop() {
        manager.stopUpdatingLocation()
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
