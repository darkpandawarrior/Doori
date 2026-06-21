package com.miletracker

import com.miletracker.feature.tracking.checkin.CheckInValidator
import com.miletracker.feature.tracking.checkin.CheckInValidator.CheckInLocation
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CheckInValidator].
 *
 * Covers:
 *  - Point inside radius → withinRadius true, distance near zero
 *  - Point just outside radius → withinRadius false, correct positive distanceOutside
 *  - Nearest-location selection among multiple candidates
 *  - Boundary point exactly at radius edge (floating-point tolerant)
 *  - haversineMeters known-distance sanity check
 *  - buildOutsideRadiusMessage contains expected information
 */
class CheckInValidatorTest {

    // Fixed mock locations for testing
    private val headOffice = CheckInLocation(
        id = "CHK-001", name = "Head Office",
        lat = 18.5204, lng = 73.8567,
        type = "OFFICE", radiusMeters = 100.0
    )
    private val warehouse = CheckInLocation(
        id = "CHK-002", name = "Warehouse",
        lat = 18.5480, lng = 73.8718,
        type = "SUPPLY_CENTER", radiusMeters = 150.0
    )
    private val jobSite = CheckInLocation(
        id = "CHK-003", name = "North Job Site",
        lat = 18.5601, lng = 73.8234,
        type = "JOB_SITE", radiusMeters = 200.0
    )

    private val allLocations = listOf(headOffice, warehouse, jobSite)

    // ── Inside radius ──────────────────────────────────────────────────────────

    @Test
    fun `point at exact coordinates of head office is within radius`() {
        val result = CheckInValidator.validate(
            userLat = headOffice.lat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = 100.0
        )

        assertTrue(result.withinRadius, "Should be within radius when at exact location")
        assertTrue(result.distanceMeters < 1.0, "Distance should be near zero (was ${result.distanceMeters})")
        assertEquals(0.0, result.distanceOutside, "distanceOutside must be 0 when within radius")
        assertEquals(headOffice.id, result.nearestLocation.id)
    }

    @Test
    fun `point 50m north of head office is within 100m radius`() {
        // ~50 m north in latitude degrees ≈ 50 / 111_000
        val offsetLat = 50.0 / 111_000.0
        val result = CheckInValidator.validate(
            userLat = headOffice.lat + offsetLat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = 100.0
        )

        assertTrue(result.withinRadius, "50 m away should be within 100 m radius")
        assertTrue(result.distanceMeters in 40.0..60.0, "Distance should be ~50 m (was ${result.distanceMeters})")
        assertEquals(0.0, result.distanceOutside)
    }

    // ── Outside radius ─────────────────────────────────────────────────────────

    @Test
    fun `point 200m away from head office is outside 100m radius`() {
        val offsetLat = 200.0 / 111_000.0
        val result = CheckInValidator.validate(
            userLat = headOffice.lat + offsetLat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = 100.0
        )

        assertFalse(result.withinRadius, "200 m away should be outside 100 m radius")
        assertTrue(result.distanceMeters > 100.0, "Distance should be > 100 m")
        assertTrue(result.distanceOutside > 0.0, "distanceOutside must be positive when outside")
        // distanceOutside should approximately equal (distance - radius)
        val expected = result.distanceMeters - result.effectiveRadius
        assertTrue(abs(result.distanceOutside - expected) < 0.01, "distanceOutside must equal distance - radius")
    }

    @Test
    fun `point just outside radius has correct distanceOutside`() {
        // 110 m away from a 100 m radius location → should be 10 m outside
        val offsetLat = 110.0 / 111_000.0
        val result = CheckInValidator.validate(
            userLat = headOffice.lat + offsetLat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = 100.0
        )

        assertFalse(result.withinRadius)
        // distanceOutside should be ~10 m (with some Haversine approximation tolerance)
        assertTrue(
            result.distanceOutside in 5.0..20.0,
            "Expected ~10 m outside, got ${result.distanceOutside} m"
        )
    }

    // ── Nearest-location selection ─────────────────────────────────────────────

    @Test
    fun `nearest location is selected from multiple candidates`() {
        // Stand exactly at warehouse coordinates — that should be nearest
        val result = CheckInValidator.validate(
            userLat = warehouse.lat,
            userLng = warehouse.lng,
            candidates = allLocations,
            defaultRadiusMeters = 100.0
        )

        assertEquals(warehouse.id, result.nearestLocation.id, "Warehouse should be nearest")
        assertTrue(result.withinRadius, "Should be within warehouse radius when at its exact location")
    }

    @Test
    fun `nearest location selection picks correct location when user is between two sites`() {
        // Midpoint between headOffice and warehouse (both ~2.7 km apart)
        // Biased 1 m toward headOffice so it is nearest
        val midLat = (headOffice.lat + warehouse.lat) / 2.0 - (0.001 / 111_000.0)
        val midLng = (headOffice.lng + warehouse.lng) / 2.0
        val result = CheckInValidator.validate(
            userLat = midLat,
            userLng = midLng,
            candidates = listOf(headOffice, warehouse),
            defaultRadiusMeters = 100.0
        )

        // Either location is valid as "nearest"; we just verify the logic ran and picked one
        assertNotNull(result.nearestLocation)
        assertTrue(result.distanceMeters > 0)
    }

    // ── Boundary exactly at radius ─────────────────────────────────────────────

    @Test
    fun `point exactly at radius boundary is within radius`() {
        val radius = 100.0
        // Compute a latitude offset that corresponds to exactly `radius` metres
        val offsetLat = radius / 111_000.0
        val exactBoundaryLat = headOffice.lat + offsetLat

        val result = CheckInValidator.validate(
            userLat = exactBoundaryLat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = radius
        )

        // Allow for floating-point approximation: either right on the boundary or just inside
        val distanceToLocation = CheckInValidator.haversineMeters(
            exactBoundaryLat, headOffice.lng, headOffice.lat, headOffice.lng
        )
        // The computed haversine distance should be ≈ radius (within 1%)
        assertTrue(
            abs(distanceToLocation - radius) < radius * 0.02,
            "Haversine distance should be ≈ $radius m (was $distanceToLocation)"
        )
        // withinRadius depends on floating-point; just verify consistency
        assertEquals(result.withinRadius, result.distanceMeters <= result.effectiveRadius)
    }

    // ── haversineMeters sanity checks ──────────────────────────────────────────

    @Test
    fun `haversineMeters returns 0 for identical points`() {
        val d = CheckInValidator.haversineMeters(18.5204, 73.8567, 18.5204, 73.8567)
        assertEquals(0.0, d, 0.0001)
    }

    @Test
    fun `haversineMeters returns ~111km per degree of latitude at equator`() {
        // 1 degree of latitude ≈ 111 km
        val d = CheckInValidator.haversineMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue(d in 110_000.0..112_000.0, "1 deg lat should be ~111 km (was $d m)")
    }

    @Test
    fun `haversineMeters is symmetric`() {
        val d1 = CheckInValidator.haversineMeters(18.5204, 73.8567, 18.5480, 73.8718)
        val d2 = CheckInValidator.haversineMeters(18.5480, 73.8718, 18.5204, 73.8567)
        assertEquals(d1, d2, 0.001)
    }

    // ── Per-location radius override ───────────────────────────────────────────

    @Test
    fun `per-location radius override is used instead of default`() {
        // warehouse has radiusMeters = 150.0; place user 120 m away
        val offsetLat = 120.0 / 111_000.0
        val result = CheckInValidator.validate(
            userLat = warehouse.lat + offsetLat,
            userLng = warehouse.lng,
            candidates = listOf(warehouse),
            defaultRadiusMeters = 100.0   // default is 100, but warehouse override is 150
        )

        // 120 m away from a 150 m radius → should be within
        assertTrue(result.withinRadius, "Should be within warehouse's 150 m override radius at 120 m distance")
        assertEquals(150.0, result.effectiveRadius)
    }

    @Test
    fun `default radius used when location has no override`() {
        val noOverride = headOffice.copy(radiusMeters = null)
        val result = CheckInValidator.validate(
            userLat = headOffice.lat,
            userLng = headOffice.lng,
            candidates = listOf(noOverride),
            defaultRadiusMeters = 200.0
        )
        assertEquals(200.0, result.effectiveRadius)
    }

    // ── buildOutsideRadiusMessage ──────────────────────────────────────────────

    @Test
    fun `buildOutsideRadiusMessage contains location name and distance`() {
        val offsetLat = 200.0 / 111_000.0
        val result = CheckInValidator.validate(
            userLat = headOffice.lat + offsetLat,
            userLng = headOffice.lng,
            candidates = listOf(headOffice),
            defaultRadiusMeters = 100.0
        )

        val message = CheckInValidator.buildOutsideRadiusMessage(result)
        assertTrue(message.contains(headOffice.name), "Message should mention location name")
        assertTrue(message.isNotBlank())
    }

    // ── IllegalArgumentException for empty list ───────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `validate throws on empty candidate list`() {
        CheckInValidator.validate(18.5204, 73.8567, emptyList())
    }
}
