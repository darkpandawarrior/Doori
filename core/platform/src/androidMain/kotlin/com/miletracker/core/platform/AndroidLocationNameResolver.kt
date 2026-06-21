package com.miletracker.core.platform

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android [LocationNameResolver] backed by the platform [Geocoder] (reverse geocoding).
 *
 * Branching:
 *  - API 33+ (Tiramisu): the async [Geocoder.getFromLocation] with a `GeocodeListener`, bridged to
 *    a suspend point via [suspendCancellableCoroutine]. The blocking overload is deprecated there.
 *  - Below 33: the blocking overload on [Dispatchers.IO].
 *
 * Every failure path — no geocoder backend ([Geocoder.isPresent] false), no result, or any
 * exception — resolves to a coords-only [PlaceName] so the caller always has a label to show. The
 * resolver itself never throws.
 *
 * NB: a real device geocoder needs network to resolve. In the offline demo, the
 * [OfflineLocationNameResolver] is bound instead; this implementation is the production path for a
 * connected device.
 */
class AndroidLocationNameResolver(private val context: Context) : LocationNameResolver {
    override suspend fun resolve(
        latitude: Double,
        longitude: Double,
    ): PlaceName {
        val fallback = PlaceName.coordinatesOnly(latitude, longitude)
        if (!Geocoder.isPresent()) return fallback

        val geocoder = Geocoder(context)
        return try {
            val addresses =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    fetchAsync(geocoder, latitude, longitude)
                } else {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)
                    }
                }
            val name = addresses?.firstOrNull()?.toShortPlaceName()
            if (name.isNullOrBlank()) fallback else PlaceName(name = name, coordinates = fallback.coordinates)
        } catch (_: Exception) {
            fallback
        }
    }

    private suspend fun fetchAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): List<Address>? =
        suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(latitude, longitude, 1) { result ->
                    if (cont.isActive) cont.resume(result)
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    /**
     * Build a short, two-part label from a resolved [Address]: the most specific available locality
     * (sub-locality / feature / thoroughfare) plus the city, e.g. "Koregaon Park, Pune". Avoids the
     * full postal address so the live label stays compact.
     */
    private fun Address.toShortPlaceName(): String? {
        val locality =
            subLocality
                ?: featureName
                ?: thoroughfare
                ?: subAdminArea
        val city = locality?.let { this.locality } ?: this.locality ?: subAdminArea
        return when {
            !locality.isNullOrBlank() && !city.isNullOrBlank() && !locality.equals(city, ignoreCase = true) ->
                "$locality, $city"
            !locality.isNullOrBlank() -> locality
            !city.isNullOrBlank() -> city
            else -> getAddressLine(0)
        }
    }
}
