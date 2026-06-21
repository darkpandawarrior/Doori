package com.miletracker.core.data.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * Lightweight Kalman-like GPS smoother — operates on lat/lng separately, uses time delta
 * to increase process uncertainty.  Pure Kotlin, no Android dependencies.
 */
class KalmanSmoother(private var processNoiseMetersPerSec: Double = 1.0) {
    private var initialized = false
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var pLat: Double = 1.0
    private var pLng: Double = 1.0
    private var lastTimestampMs: Long = 0L

    fun reset() {
        initialized = false
        lat = 0.0
        lng = 0.0
        pLat = 1.0
        pLng = 1.0
        lastTimestampMs = 0L
    }

    fun setProcessNoiseMetersPerSec(mps: Double) {
        processNoiseMetersPerSec = mps.coerceAtLeast(0.0)
    }

    /**
     * Smooth a new GPS measurement.
     * @return Pair(smoothedLat, smoothedLng)
     */
    fun smooth(
        measuredLat: Double,
        measuredLng: Double,
        accuracyMeters: Float,
        timestampMs: Long,
    ): Pair<Double, Double> {
        val measNoiseLat = metersToLatDeg(accuracyMeters.toDouble())
        val measNoiseLng = metersToLngDeg(accuracyMeters.toDouble(), measuredLat)

        if (!initialized) {
            lat = measuredLat
            lng = measuredLng
            pLat = measNoiseLat * measNoiseLat
            pLng = measNoiseLng * measNoiseLng
            lastTimestampMs = timestampMs
            initialized = true
            return Pair(lat, lng)
        }

        val dtSec = max(0.0, (timestampMs - lastTimestampMs).coerceAtLeast(0L) / 1000.0)
        val processStd = processNoiseMetersPerSec * dtSec
        val qLat = metersToLatDeg(processStd)
        val qLng = metersToLngDeg(processStd, lat)

        pLat += qLat * qLat
        pLng += qLng * qLng

        val rLat = measNoiseLat * measNoiseLat
        val rLng = measNoiseLng * measNoiseLng
        val kLat = if (pLat + rLat > 0) pLat / (pLat + rLat) else 0.0
        val kLng = if (pLng + rLng > 0) pLng / (pLng + rLng) else 0.0

        lat += kLat * (measuredLat - lat)
        lng += kLng * (measuredLng - lng)
        pLat = (1 - kLat) * pLat
        pLng = (1 - kLng) * pLng
        lastTimestampMs = timestampMs
        return Pair(lat, lng)
    }

    private fun metersToLatDeg(meters: Double): Double = meters / 111320.0

    private fun metersToLngDeg(
        meters: Double,
        atLatDeg: Double,
    ): Double {
        val metersPerDeg = 111320.0 * cos(atLatDeg * PI / 180.0)
        return if (metersPerDeg <= 0.0) meters / 111320.0 else meters / metersPerDeg
    }
}
