package com.miletracker

import com.miletracker.core.data.model.db.LocationData
import com.miletracker.feature.tracking.map.MapRouteBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MapRouteBuilder].  Pure JVM — no Android device needed.
 *
 * Covers:
 * - Empty input
 * - Route coordinate order and completeness
 * - Start / end coordinate selection
 * - Abnormal / filtered partitioning
 * - Bounding-box computation
 * - Mixed-category inputs
 */
class MapRouteBuilderTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun loc(
        lat: Double,
        lng: Double,
        isAbnormal: Boolean = false,
        isMock: Boolean = false,
        isPaused: Boolean = false
    ) = LocationData(
        activity = "DRIVING",
        speed = 10f,
        lat = lat,
        lng = lng,
        token = "test-token",
        batteryPercentage = 80.0,
        isAbnormal = isAbnormal,
        isMock = isMock,
        isPaused = isPaused
    )

    // -----------------------------------------------------------------------
    // Empty-input contract
    // -----------------------------------------------------------------------

    @Test
    fun `empty input produces empty result`() {
        val result = MapRouteBuilder.build(emptyList())

        assertTrue(result.routeCoords.isEmpty())
        assertNull(result.startCoord)
        assertNull(result.endCoord)
        assertTrue(result.filteredCoords.isEmpty())
        assertTrue(result.abnormalCoords.isEmpty())
        assertTrue(result.bounds.isEmpty)
    }

    // -----------------------------------------------------------------------
    // Route coordinate order
    // -----------------------------------------------------------------------

    @Test
    fun `normal points appear in route in original list order`() {
        val points = listOf(
            loc(1.0, 10.0),
            loc(2.0, 20.0),
            loc(3.0, 30.0)
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(3, result.routeCoords.size)
        assertEquals(MapRouteBuilder.LatLng(1.0, 10.0), result.routeCoords[0])
        assertEquals(MapRouteBuilder.LatLng(2.0, 20.0), result.routeCoords[1])
        assertEquals(MapRouteBuilder.LatLng(3.0, 30.0), result.routeCoords[2])
    }

    // -----------------------------------------------------------------------
    // Start / end selection — normal route
    // -----------------------------------------------------------------------

    @Test
    fun `start is first normal point and end is last normal point`() {
        val points = listOf(
            loc(1.0, 10.0),
            loc(2.0, 20.0),
            loc(3.0, 30.0)
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(MapRouteBuilder.LatLng(1.0, 10.0), result.startCoord)
        assertEquals(MapRouteBuilder.LatLng(3.0, 30.0), result.endCoord)
    }

    @Test
    fun `single normal point makes start equal to end`() {
        val result = MapRouteBuilder.build(listOf(loc(5.0, 50.0)))

        assertNotNull(result.startCoord)
        assertNotNull(result.endCoord)
        assertEquals(result.startCoord, result.endCoord)
    }

    // -----------------------------------------------------------------------
    // Start / end fallback when all points are filtered or abnormal
    // -----------------------------------------------------------------------

    @Test
    fun `when all points are abnormal start falls back to first point`() {
        val points = listOf(
            loc(1.0, 10.0, isAbnormal = true),
            loc(2.0, 20.0, isAbnormal = true)
        )
        val result = MapRouteBuilder.build(points)

        assertTrue(result.routeCoords.isEmpty())
        assertEquals(MapRouteBuilder.LatLng(1.0, 10.0), result.startCoord)
        assertEquals(MapRouteBuilder.LatLng(2.0, 20.0), result.endCoord)
    }

    @Test
    fun `when all points are mock start falls back to first point`() {
        val points = listOf(
            loc(3.0, 30.0, isMock = true),
            loc(4.0, 40.0, isMock = true)
        )
        val result = MapRouteBuilder.build(points)

        assertTrue(result.routeCoords.isEmpty())
        assertEquals(MapRouteBuilder.LatLng(3.0, 30.0), result.startCoord)
        assertEquals(MapRouteBuilder.LatLng(4.0, 40.0), result.endCoord)
    }

    // -----------------------------------------------------------------------
    // Partitioning — abnormal points
    // -----------------------------------------------------------------------

    @Test
    fun `abnormal points go to abnormalCoords not routeCoords`() {
        val points = listOf(
            loc(1.0, 10.0),
            loc(2.0, 20.0, isAbnormal = true),
            loc(3.0, 30.0)
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(2, result.routeCoords.size)
        assertEquals(1, result.abnormalCoords.size)
        assertTrue(result.filteredCoords.isEmpty())

        assertEquals(MapRouteBuilder.LatLng(2.0, 20.0), result.abnormalCoords[0])
        // Route coords must not contain the abnormal point.
        assertFalse(result.routeCoords.any { it.lat == 2.0 })
    }

    // -----------------------------------------------------------------------
    // Partitioning — mock (filtered) points
    // -----------------------------------------------------------------------

    @Test
    fun `mock points go to filteredCoords not routeCoords`() {
        val points = listOf(
            loc(1.0, 10.0),
            loc(2.0, 20.0, isMock = true),
            loc(3.0, 30.0)
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(2, result.routeCoords.size)
        assertEquals(1, result.filteredCoords.size)
        assertTrue(result.abnormalCoords.isEmpty())

        assertEquals(MapRouteBuilder.LatLng(2.0, 20.0), result.filteredCoords[0])
    }

    @Test
    fun `paused points go to filteredCoords`() {
        val points = listOf(
            loc(1.0, 10.0),
            loc(2.0, 20.0, isPaused = true)
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(1, result.routeCoords.size)
        assertEquals(1, result.filteredCoords.size)
        assertEquals(MapRouteBuilder.LatLng(2.0, 20.0), result.filteredCoords[0])
    }

    // -----------------------------------------------------------------------
    // Bounding-box computation
    // -----------------------------------------------------------------------

    @Test
    fun `bounds cover all normal points`() {
        val points = listOf(
            loc(10.0, 20.0),
            loc(15.0, 25.0),
            loc(12.0, 22.0)
        )
        val result = MapRouteBuilder.build(points)

        assertFalse(result.bounds.isEmpty)
        assertEquals(10.0, result.bounds.minLat, 1e-9)
        assertEquals(15.0, result.bounds.maxLat, 1e-9)
        assertEquals(20.0, result.bounds.minLng, 1e-9)
        assertEquals(25.0, result.bounds.maxLng, 1e-9)
    }

    @Test
    fun `bounds include abnormal and filtered points`() {
        val points = listOf(
            loc(10.0, 20.0),
            loc(50.0, 80.0, isAbnormal = true),   // extreme outlier — still included in bounds
            loc(5.0,  15.0, isMock = true)
        )
        val result = MapRouteBuilder.build(points)

        assertFalse(result.bounds.isEmpty)
        assertEquals(5.0,  result.bounds.minLat, 1e-9)
        assertEquals(50.0, result.bounds.maxLat, 1e-9)
        assertEquals(15.0, result.bounds.minLng, 1e-9)
        assertEquals(80.0, result.bounds.maxLng, 1e-9)
    }

    @Test
    fun `single-point bounds have identical min and max`() {
        val result = MapRouteBuilder.build(listOf(loc(7.0, 8.0)))

        assertEquals(7.0, result.bounds.minLat, 1e-9)
        assertEquals(7.0, result.bounds.maxLat, 1e-9)
        assertEquals(8.0, result.bounds.minLng, 1e-9)
        assertEquals(8.0, result.bounds.maxLng, 1e-9)
    }

    // -----------------------------------------------------------------------
    // Mixed-category inputs
    // -----------------------------------------------------------------------

    @Test
    fun `mixed normal abnormal mock points partition correctly`() {
        val points = listOf(
            loc(1.0, 1.0),                          // normal
            loc(2.0, 2.0, isAbnormal = true),       // abnormal
            loc(3.0, 3.0, isMock = true),           // filtered
            loc(4.0, 4.0),                          // normal
            loc(5.0, 5.0, isPaused = true),         // filtered
            loc(6.0, 6.0, isAbnormal = true)        // abnormal
        )
        val result = MapRouteBuilder.build(points)

        assertEquals(2, result.routeCoords.size,    "Expected 2 normal points")
        assertEquals(2, result.abnormalCoords.size, "Expected 2 abnormal points")
        assertEquals(2, result.filteredCoords.size, "Expected 2 filtered points")

        // Route must contain exactly the two normal points.
        assertEquals(MapRouteBuilder.LatLng(1.0, 1.0), result.routeCoords[0])
        assertEquals(MapRouteBuilder.LatLng(4.0, 4.0), result.routeCoords[1])

        // Start/end from normal route.
        assertEquals(MapRouteBuilder.LatLng(1.0, 1.0), result.startCoord)
        assertEquals(MapRouteBuilder.LatLng(4.0, 4.0), result.endCoord)
    }

    // -----------------------------------------------------------------------
    // Internal helper: computeBounds (white-box)
    // -----------------------------------------------------------------------

    @Test
    fun `computeBounds returns EMPTY for empty list`() {
        val bounds = MapRouteBuilder.computeBounds(emptyList())
        assertTrue(bounds.isEmpty)
    }

    @Test
    fun `computeBounds handles negative coordinates`() {
        val coords = listOf(
            MapRouteBuilder.LatLng(-10.0, -20.0),
            MapRouteBuilder.LatLng(-5.0,  -15.0)
        )
        val bounds = MapRouteBuilder.computeBounds(coords)

        assertEquals(-10.0, bounds.minLat, 1e-9)
        assertEquals(-5.0,  bounds.maxLat, 1e-9)
        assertEquals(-20.0, bounds.minLng, 1e-9)
        assertEquals(-15.0, bounds.maxLng, 1e-9)
    }
}
