package com.mileway

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.mileway.feature.tracking.service.location.GpsFix
import com.mileway.feature.tracking.service.location.LocationSource

/** Re-registering the provider below this much of a cadence change churns it for no benefit. */
private const val MinIntervalChangeMs = 1_000L

/**
 * noGms/F-Droid flavor [LocationSource]: plain GPS via the platform
 * [android.location.LocationManager], no Play Services.
 *
 * L13: recovers the flavor-gated pure-GPS tracker (PR #85, lost when a background agent's
 * worktree was deleted). Reads straight from `GPS_PROVIDER` — no fused/Wi-Fi/cell blend, so fixes
 * arrive a little slower to first-lock than [GmsFusedLocationSource] and never fall back to
 * network positioning indoors. That is the deliberate FOSS trade: no `com.google.android.gms.*`
 * import anywhere in this class. Same per-flavor split as [HeuristicActivityRecognizer] /
 * `ZxingBarcodeDecoder`: bound in `PlatformServicesKoinEntry.kt` via `RealLocationSourceFactory`.
 *
 * `forceGpsOnly` (the gms flavor's "force GPS provider" setting, P10.1) is meaningless here — this
 * source is already GPS-only — so [RealLocationSourceFactory] on this flavor ignores it.
 */
class PlainLocationTracker(
    private val context: Context,
    private val initialIntervalMs: Long = 4_000L,
) : LocationSource {
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private var listener: LocationListener? = null
    private var onFix: ((GpsFix) -> Unit)? = null
    private var currentIntervalMs: Long = initialIntervalMs

    override fun start(onFix: (GpsFix) -> Unit) {
        this.onFix = onFix
        register(currentIntervalMs)
    }

    override fun updateInterval(intervalMs: Long) {
        if (onFix == null) return // not started
        if (kotlin.math.abs(intervalMs - currentIntervalMs) < MinIntervalChangeMs) return
        currentIntervalMs = intervalMs
        removeUpdates()
        register(intervalMs)
    }

    private fun register(intervalMs: Long) {
        val fix = onFix ?: return
        val l = LocationListener { location -> fix(location.toGpsFix()) }
        listener = l
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMs,
                0f,
                l,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            // Permission revoked mid-session; the service handles the empty stream.
        } catch (_: IllegalArgumentException) {
            // GPS_PROVIDER not present on this device; empty stream, service falls back.
        }
    }

    private fun removeUpdates() {
        listener?.let { locationManager.removeUpdates(it) }
        listener = null
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
            provider = provider ?: LocationManager.GPS_PROVIDER,
            isMock =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isMock
                } else {
                    @Suppress("DEPRECATION")
                    isFromMockProvider
                },
        )
}
