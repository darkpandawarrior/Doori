@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.service.location

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Abstracts the stream of GPS fixes so the service can use real GPS or a simulated drive. */
interface LocationSource {
    fun start(onFix: (GpsFix) -> Unit)

    fun stop()

    /**
     * Request a new location-update cadence (C.2a). The service recomputes this per fix via
     * [DynamicIntervalCalculator]: faster movement shortens it, low battery / power-saver / long
     * sessions stretch it. Default no-op for sources with a fixed cadence (e.g. the simulator).
     */
    fun updateInterval(intervalMs: Long) = Unit
}

/**
 * Real GPS via the fused location provider.
 *
 * P10.1: [forceGpsOnly] (Track Miles "force GPS provider" setting) swaps the fused client for the
 * platform [android.location.LocationManager] GPS_PROVIDER, so fixes come straight from the GNSS
 * hardware with no Wi-Fi/cell fusion. Default false keeps the fused high-accuracy behavior.
 */
class FusedLocationSource(
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
        if (kotlin.math.abs(intervalMs - currentIntervalMs) < 1_000L) return
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
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
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

/**
 * Emits a believable driving route for the offline demo: ~22 m steps every 2 s with gentle
 * heading drift and small positional jitter, feeding the same advanced pipeline as real GPS.
 * Occasionally flags an on-route point as mock-sourced so mock handling is observable.
 */
class SimulatedLocationSource(
    // Pune city center coordinates
    private val startLat: Double = 18.5204,
    private val startLng: Double = 73.8567,
    private val intervalMs: Long = 2_000L,
) : LocationSource {
    private var scope: CoroutineScope? = null

    override fun start(onFix: (GpsFix) -> Unit) {
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        s.launch {
            var lat = startLat
            var lng = startLng
            var bearing = 45.0
            var step = 0
            while (isActive) {
                val speed = (8.0 + Math.random() * 6.0) // 8–14 m/s (~29–50 km/h)
                val now = System.currentTimeMillis()
                val isMock = step > 0 && step % 20 == 0 // periodic mock-sourced point
                onFix(
                    GpsFix(
                        lat = lat + (Math.random() - 0.5) * 0.00002,
                        lng = lng + (Math.random() - 0.5) * 0.00002,
                        timeMs = now,
                        speedMps = speed.toFloat(),
                        accuracyM = (4.0 + Math.random() * 4.0).toFloat(),
                        bearingDeg = bearing.toFloat(),
                        altitudeM = 560.0,
                        provider = "fused",
                        isMock = isMock,
                    ),
                )
                // Advance along the current bearing by speed * dt.
                val distanceM = speed * (intervalMs / 1000.0)
                val bearingRad = Math.toRadians(bearing)
                lat += (distanceM * cos(bearingRad)) / 111_320.0
                lng += (distanceM * sin(bearingRad)) / (111_320.0 * cos(Math.toRadians(lat)))
                bearing += (Math.random() - 0.5) * 20.0 // gentle curve
                step++
                delay(intervalMs)
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
    }
}
