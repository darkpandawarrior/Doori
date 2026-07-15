package com.mileway.core.data.util

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Canonical great-circle (haversine) distance in metres between two lat/lng points.
 *
 * This is the single source of truth for straight-line distance across Mileway's modules —
 * `core:data` sits below every feature module that needs it (tracking, logging, stub), so this
 * is the shared home rather than duplicating the formula per module. Callers that need
 * kilometres should divide the result, not reimplement the formula.
 */
fun haversineMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLng / 2) * sin(dLng / 2)
    return earthRadiusM * (2 * atan2(sqrt(a), sqrt(1 - a)))
}
