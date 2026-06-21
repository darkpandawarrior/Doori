package com.miletracker.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

/**
 * iOS [LocationNameResolver] via CoreLocation's [CLGeocoder] (the CLGeocoder counterpart to
 * Android's `Geocoder`). `reverseGeocodeLocation` is asynchronous and callback-based; it is bridged
 * to a suspend point with [suspendCancellableCoroutine].
 *
 * Every failure path — geocoder error, empty placemark list, or a placemark with no usable
 * locality — resolves to a coords-only [PlaceName], so the caller always has a label. Never throws.
 *
 * As on Android, the real geocoder needs network; the offline demo binds
 * [OfflineLocationNameResolver] instead. This is the connected-device path.
 */
class IosLocationNameResolver : LocationNameResolver {
    override suspend fun resolve(
        latitude: Double,
        longitude: Double,
    ): PlaceName {
        val fallback = PlaceName.coordinatesOnly(latitude, longitude)
        val geocoder = CLGeocoder()
        val location = CLLocation(latitude = latitude, longitude = longitude)

        val placemark: CLPlacemark? =
            suspendCancellableCoroutine { cont ->
                geocoder.reverseGeocodeLocation(location) { placemarks, _ ->
                    val first = (placemarks?.firstOrNull()) as? CLPlacemark
                    if (cont.isActive) cont.resume(first)
                }
                cont.invokeOnCancellation { geocoder.cancelGeocode() }
            }

        val name = placemark?.toShortPlaceName()
        return if (name.isNullOrBlank()) fallback else PlaceName(name = name, coordinates = fallback.coordinates)
    }

    /** Short "<sub-locality>, <city>" label from a placemark, mirroring the Android formatter. */
    private fun CLPlacemark.toShortPlaceName(): String? {
        val locality = subLocality ?: name ?: thoroughfare ?: subAdministrativeArea
        val city = this.locality ?: subAdministrativeArea
        return when {
            !locality.isNullOrBlank() && !city.isNullOrBlank() && !locality.equals(city, ignoreCase = true) ->
                "$locality, $city"
            !locality.isNullOrBlank() -> locality
            !city.isNullOrBlank() -> city
            else -> null
        }
    }
}
