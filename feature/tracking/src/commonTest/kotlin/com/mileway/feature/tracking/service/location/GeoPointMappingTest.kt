package com.mileway.feature.tracking.service.location

import com.siddharth.kmp.appshell.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The CLLocation -> pipeline mapping. Before this, the iOS distance pipeline's only evidence was
 * "xcodebuild compiled it" — see PLAN_V33 C3.
 *
 * The behaviour under test is CoreLocation's convention that a **negative** speed or course means
 * "not available" rather than an actual value.
 */
class GeoPointMappingTest {
    @Test
    fun `a fully populated fix maps every field straight through`() {
        val fix =
            GeoPoint(
                latitude = 18.5204,
                longitude = 73.8567,
                accuracyMeters = 7.5f,
                timestampMillis = 1_700_000_000_000L,
                speedMetersPerSecond = 12.5f,
                courseDegrees = 271.25,
                altitudeMeters = 560.0,
            ).toGpsFix()

        assertEquals(18.5204, fix.lat)
        assertEquals(73.8567, fix.lng)
        assertEquals(1_700_000_000_000L, fix.timeMs)
        assertEquals(12.5f, fix.speedMps)
        assertEquals(7.5f, fix.accuracyM)
        assertEquals(271.25f, fix.bearingDeg)
        assertEquals(560.0, fix.altitudeM)
        assertEquals("ios", fix.provider)
    }

    @Test
    fun `the negative speed sentinel is clamped rather than propagated`() {
        // -1f is GeoPoint's default and CoreLocation's "speed unavailable" marker. A negative speed
        // reaching the pipeline corrupts jitter gating and the adaptive sampling interval.
        assertEquals(0f, point(speed = -1f).toGpsFix().speedMps)
    }

    @Test
    fun `the negative course sentinel is clamped rather than propagated`() {
        assertEquals(0f, point(course = -1.0).toGpsFix().bearingDeg)
    }

    @Test
    fun `a zero speed survives, because standing still is a real reading`() {
        // Guards against the clamp being written as `<= 0`: only negative is the sentinel.
        assertEquals(0f, point(speed = 0f).toGpsFix().speedMps)
    }

    @Test
    fun `a zero course survives, because due north is a real bearing`() {
        assertEquals(0f, point(course = 0.0).toGpsFix().bearingDeg)
    }

    @Test
    fun `both sentinels clamp independently and leave the rest of the fix intact`() {
        val fix = point(speed = -1f, course = -1.0).toGpsFix()
        assertEquals(0f, fix.speedMps)
        assertEquals(0f, fix.bearingDeg)
        assertEquals(18.5204, fix.lat)
        assertEquals("ios", fix.provider)
    }

    @Test
    fun `a negative altitude is preserved, unlike the speed and course sentinels`() {
        // Below sea level is a legitimate reading, so altitude must NOT be clamped.
        val fix = point().copy(altitudeMeters = -12.0, accuracyMeters = 3.25f).toGpsFix()
        assertEquals(-12.0, fix.altitudeM)
        assertEquals(3.25f, fix.accuracyM)
    }

    private fun point(
        speed: Float = 5f,
        course: Double = 90.0,
    ) = GeoPoint(
        latitude = 18.5204,
        longitude = 73.8567,
        accuracyMeters = 5f,
        timestampMillis = 1_700_000_000_000L,
        speedMetersPerSecond = speed,
        courseDegrees = course,
        altitudeMeters = 100.0,
    )
}
