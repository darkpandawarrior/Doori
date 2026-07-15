package com.mileway.core.data.util

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the canonical [haversineMeters] against known coordinate pairs. Every module-specific
 * haversine duplicate (feature:tracking's CheckInValidator/GeoCheckInScreen, feature:logging's
 * haversineKm, stub's FakeTrackingNetworkApi) now delegates here — a regression in this formula
 * would silently ripple through all of them.
 */
class GeoMathTest {
    @Test
    fun `identical points are zero distance`() {
        val d = haversineMeters(18.5204, 73.8567, 18.5204, 73.8567)
        assertEquals(0.0, d, 0.0001)
    }

    @Test
    fun `known pair Pune Railway Station to Pune Airport is about 6_7km`() {
        // Pune Railway Station (18.5286, 73.8743) -> Pune Airport (18.5793, 73.9089): ~6.71 km great-circle.
        val d = haversineMeters(18.5286, 73.8743, 18.5793, 73.9089)
        assertTrue(d in 6_600.0..6_800.0, "expected ~6.71km (6600-6800m), got ${d}m")
    }

    @Test
    fun `one degree of latitude is about 111km`() {
        val d = haversineMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue(d in 110_000.0..112_000.0, "1 deg lat should be ~111km, got ${d}m")
    }

    @Test
    fun `is symmetric`() {
        val d1 = haversineMeters(18.5204, 73.8567, 18.5480, 73.8718)
        val d2 = haversineMeters(18.5480, 73.8718, 18.5204, 73.8567)
        assertTrue(abs(d1 - d2) < 0.001)
    }
}
