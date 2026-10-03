@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.service.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Positional jitter per fix, in degrees — about 2 m. */
private const val JitterDegrees = 0.00002

/** Metres per degree of latitude — the flat-earth step the simulator walks with. */
private const val MetresPerDegreeLatitude = 111_320.0

/** `Math.random()` is 0..1; subtracting this centres it on zero. */
private const val RandomCentre = 0.5

/** Heading wanders by up to ±10° per fix, which is `(random - 0.5) * 20`. */
private const val BearingDriftSpanDegrees = 20.0

/** Abstracts the stream of GPS fixes so the service can use real GPS or a simulated drive. */
interface LocationSource {
    fun start(onFix: (GpsFix) -> Unit)

    fun stop()

    /**
     * Request a new location-update cadence (C.2a). The service recomputes this per fix via
     * [com.siddharth.kmp.location.DynamicIntervalCalculator]: faster movement shortens it, low battery / power-saver / long
     * sessions stretch it. Default no-op for sources with a fixed cadence (e.g. the simulator).
     */
    fun updateInterval(intervalMs: Long) = Unit
}

/**
 * L13: builds the real (non-simulated) [LocationSource] for the current build flavor, so
 * [LocationTrackingService] never imports a flavor-specific implementation directly. Bound in
 * `PlatformServicesKoinEntry.kt` — gms → `GmsFusedLocationSource` (Play Services fused provider,
 * `app/src/gms`), noGms → `PlainLocationTracker` (plain `android.location.LocationManager`
 * GPS_PROVIDER, `app/src/noGms`). Same per-flavor split as [ActivityRecognizer] / `BarcodeDecoder`.
 */
fun interface RealLocationSourceFactory {
    /**
     * @param forceGpsOnly P10.1 Track Miles setting: swap the fused client for the raw GPS
     * provider. Meaningful only on the gms flavor's fused implementation — the noGms
     * implementation is already GPS-only, so it ignores this flag.
     */
    fun create(
        forceGpsOnly: Boolean,
        initialIntervalMs: Long,
    ): LocationSource
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
                        lat = lat + (Math.random() - RandomCentre) * JitterDegrees,
                        lng = lng + (Math.random() - RandomCentre) * JitterDegrees,
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
                lat += (distanceM * cos(bearingRad)) / MetresPerDegreeLatitude
                lng += (distanceM * sin(bearingRad)) / (MetresPerDegreeLatitude * cos(Math.toRadians(lat)))
                bearing += (Math.random() - RandomCentre) * BearingDriftSpanDegrees // gentle curve
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
