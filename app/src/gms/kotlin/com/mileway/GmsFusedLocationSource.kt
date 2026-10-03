@file:Suppress("ktlint:standard:max-line-length")

package com.mileway

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mileway.feature.tracking.service.location.GpsFix
import com.mileway.feature.tracking.service.location.LocationSource

/** Re-registering the provider below this much of a cadence change churns it for no benefit. */
private const val MinIntervalChangeMs = 1_000L

/**
 * gms flavor [LocationSource]: real GPS via the fused location provider.
 *
 * L13: moved here from feature/tracking's shared androidMain (recovering PR #85's flavor split,
 * lost when a background agent's worktree was deleted) — it was importing
 * `com.google.android.gms.location.*` unconditionally, leaking Play Services into the noGms/F-Droid
 * classpath. Same per-flavor split as [GmsActivityRecognizer] / `MlKitBarcodeDecoder`: bound in
 * `PlatformServicesKoinEntry.kt` via `RealLocationSourceFactory`, gms → this, noGms →
 * `PlainLocationTracker` (`app/src/noGms`).
 *
 * P10.1: [forceGpsOnly] (Track Miles "force GPS provider" setting) swaps the fused client for the
 * platform [android.location.LocationManager] GPS_PROVIDER, so fixes come straight from the GNSS
 * hardware with no Wi-Fi/cell fusion. Default false keeps the fused high-accuracy behavior.
 */
class GmsFusedLocationSource(
    private val context: Context,
    private val initialIntervalMs: Long = 4_000L,
    private val forceGpsOnly: Boolean = false,
) : LocationSource {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null
    private var onFix: ((GpsFix) -> Unit)? = null
    private var currentIntervalMs: Long = initialIntervalMs

    // P10.1: raw-GPS path.
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    }
    private var rawGpsListener: android.location.LocationListener? = null

    override fun start(onFix: (GpsFix) -> Unit) {
        this.onFix = onFix
        register(currentIntervalMs)
    }

    /**
     * Re-register the request only when the cadence actually moves (≥1s), re-registering on
     * every fix would churn the provider for no benefit. Removes the old callback first so a single
     * callback is ever active.
     */
    override fun updateInterval(intervalMs: Long) {
        if (onFix == null) return // not started
        if (kotlin.math.abs(intervalMs - currentIntervalMs) < MinIntervalChangeMs) return
        currentIntervalMs = intervalMs
        removeUpdates()
        register(intervalMs)
    }

    private fun register(intervalMs: Long) {
        val fix = onFix ?: return
        if (forceGpsOnly) {
            registerRawGps(intervalMs, fix)
            return
        }
        val request =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()
        val cb =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { fix(it.toGpsFix()) }
                }
            }
        callback = cb
        try {
            client.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Permission revoked mid-session; the service handles the empty stream.
        }
    }

    private fun registerRawGps(
        intervalMs: Long,
        fix: (GpsFix) -> Unit,
    ) {
        val listener =
            android.location.LocationListener { location -> fix(location.toGpsFix()) }
        rawGpsListener = listener
        try {
            locationManager.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER,
                intervalMs,
                0f,
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            // Permission revoked mid-session; the service handles the empty stream.
        } catch (_: IllegalArgumentException) {
            // GPS_PROVIDER not present on this device; empty stream, service falls back.
        }
    }

    private fun removeUpdates() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        rawGpsListener?.let { locationManager.removeUpdates(it) }
        rawGpsListener = null
    }

    override fun stop() {
        removeUpdates()
        onFix = null
    }

    private fun Location.toGpsFix(): GpsFix =
        GpsFix(
            lat = latitude,
            lng = longitude,
            timeMs = time,
            speedMps = speed,
            accuracyM = accuracy,
            bearingDeg = bearing,
            altitudeM = altitude,
            provider = provider ?: "fused",
            isMock =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isMock
                } else {
                    @Suppress("DEPRECATION")
                    isFromMockProvider
                },
        )
}
