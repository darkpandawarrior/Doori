package com.mileway.core.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the canonical [haversineMeters] — the earth-radius constant and a known coordinate pair —
 * so a re-drift back to per-module duplicates (`:server`'s `haversineKm`, `app-web-preview`'s
 * wasmJs-only copy) doesn't silently change the shared formula underneath both consumers.
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
}
