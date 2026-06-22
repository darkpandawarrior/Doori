package com.miletracker.feature.tracking.service.location

import com.miletracker.core.data.model.db.LocationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceCalculatorTest {
    private fun pt(
        lat: Double,
        lng: Double,
        isAbnormal: Boolean = false,
        isMock: Boolean = false,
        isPaused: Boolean = false,
    ) = LocationData(
        activity = "DRIVING",
        speed = 11f,
        lat = lat,
        lng = lng,
        token = "tok",
        batteryPercentage = 100.0,
        isAbnormal = isAbnormal,
        isMock = isMock,
        isPaused = isPaused,
    )

    @Test
    fun `empty list returns zero`() {
        assertEquals(0.0, DistanceCalculator.computeCleanedDistance(emptyList()), 0.001)
    }

    @Test
    fun `single point returns zero`() {
        assertEquals(0.0, DistanceCalculator.computeCleanedDistance(listOf(pt(18.5, 73.8))), 0.001)
    }

    @Test
    fun `two clean points returns their haversine distance`() {
        val expected = haversineMeters(18.5000, 73.8, 18.5010, 73.8)
        val result = DistanceCalculator.computeCleanedDistance(listOf(pt(18.5000, 73.8), pt(18.5010, 73.8)))
        assertEquals(expected, result, 0.01)
        assertTrue(result in 105.0..115.0, "expected ~111 m, got $result")
    }

    @Test
    fun `abnormal points are excluded from distance`() {
        val spike = pt(18.6000, 73.8, isAbnormal = true)
        val points = listOf(pt(18.5000, 73.8), spike, pt(18.5010, 73.8))
        val result = DistanceCalculator.computeCleanedDistance(points)
        assertTrue(result in 105.0..115.0, "teleport should be excluded, got $result")
    }

    @Test
    fun `mock points are excluded from distance`() {
        val mock = pt(18.5010, 73.8, isMock = true)
        val points = listOf(pt(18.5000, 73.8), mock, pt(18.5020, 73.8))
        val result = DistanceCalculator.computeCleanedDistance(points)
        assertTrue(result in 210.0..230.0, "mock should be excluded, got $result")
    }

    @Test
    fun `paused points are excluded from distance`() {
        val paused = pt(18.5010, 73.8, isPaused = true)
        val points = listOf(pt(18.5000, 73.8), paused, pt(18.5020, 73.8))
        val result = DistanceCalculator.computeCleanedDistance(points)
        assertTrue(result in 210.0..230.0, "paused should be excluded, got $result")
    }

    @Test
    fun `all points excluded gives zero`() {
        val points = listOf(pt(18.5000, 73.8, isAbnormal = true), pt(18.5010, 73.8, isMock = true))
        assertEquals(0.0, DistanceCalculator.computeCleanedDistance(points), 0.001)
    }

    @Test
    fun `three clean points sum correctly`() {
        val d01 = haversineMeters(18.5000, 73.8, 18.5010, 73.8)
        val d12 = haversineMeters(18.5010, 73.8, 18.5020, 73.8)
        val points = listOf(pt(18.5000, 73.8), pt(18.5010, 73.8), pt(18.5020, 73.8))
        val result = DistanceCalculator.computeCleanedDistance(points)
        assertEquals(d01 + d12, result, 0.01)
    }
}
