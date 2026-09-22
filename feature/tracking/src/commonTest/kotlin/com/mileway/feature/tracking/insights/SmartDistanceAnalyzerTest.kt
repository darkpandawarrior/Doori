package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.LocationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartDistanceAnalyzerTest {
    private fun point(
        speed: Float = 11f,
        isAbnormal: Boolean = false,
        isMock: Boolean = false,
        isPaused: Boolean = false,
        wasManuallyPaused: Boolean = false,
        wasBatteryOptimizationEnabled: Boolean = false,
        wasPowerSaverModeEnabled: Boolean = false,
    ) = LocationData(
        activity = "DRIVING",
        speed = speed,
        lat = 0.0,
        lng = 0.0,
        token = "tok",
        batteryPercentage = 100.0,
        isAbnormal = isAbnormal,
        isMock = isMock,
        isPaused = isPaused,
        wasManuallyPaused = wasManuallyPaused,
        wasBatteryOptimizationEnabled = wasBatteryOptimizationEnabled,
        wasPowerSaverModeEnabled = wasPowerSaverModeEnabled,
    )

    private val emptyBuckets =
        DistanceBucketBreakdown(
            originalDistance = 0.0,
            cleanedDistance = 0.0,
            mockDistance = 0.0,
            abnormalDistance = 0.0,
            spikeDistance = 0.0,
        )

    // -- per-category reduction options ---------------------------------------------------

    private data class CategoryCase(
        val name: String,
        val category: ReductionCategory,
        val buckets: DistanceBucketBreakdown,
        val points: List<LocationData>,
        val expectedReduction: Double,
        val expectMinConfidence: Double,
    )

    @Test
    fun `each category produces the expected reduction and confidence`() {
        val cases =
            listOf(
                CategoryCase(
                    name = "walking: half the points are sub-2m-s and unpaused",
                    category = ReductionCategory.WALKING,
                    buckets = emptyBuckets.copy(originalDistance = 10.0),
                    points = listOf(point(speed = 1f), point(speed = 1f), point(speed = 20f), point(speed = 20f)),
                    expectedReduction = 5.0,
                    expectMinConfidence = 0.5,
                ),
                CategoryCase(
                    name = "stationary-drift: quarter of points below 0.5 m/s",
                    category = ReductionCategory.STATIONARY_DRIFT,
                    buckets = emptyBuckets.copy(originalDistance = 8.0),
                    points = listOf(point(speed = 0.1f), point(speed = 20f), point(speed = 20f), point(speed = 20f)),
                    expectedReduction = 2.0,
                    expectMinConfidence = 0.25,
                ),
                CategoryCase(
                    name = "speed-outlier: reuses the abnormal-distance bucket directly",
                    category = ReductionCategory.SPEED_OUTLIER,
                    buckets = emptyBuckets.copy(originalDistance = 10.0, abnormalDistance = 3.5),
                    points = listOf(point(isAbnormal = true), point(isAbnormal = false)),
                    expectedReduction = 3.5,
                    expectMinConfidence = 0.5,
                ),
                CategoryCase(
                    name = "battery-opt: reuses the spike-distance bucket when any point affected",
                    category = ReductionCategory.BATTERY_OPT,
                    buckets = emptyBuckets.copy(originalDistance = 10.0, spikeDistance = 1.2),
                    points = listOf(point(wasBatteryOptimizationEnabled = true), point(), point(), point()),
                    expectedReduction = 1.2,
                    expectMinConfidence = 0.25,
                ),
                CategoryCase(
                    name = "pause-drift: half the points paused",
                    category = ReductionCategory.PAUSE_DRIFT,
                    buckets = emptyBuckets.copy(originalDistance = 6.0),
                    points = listOf(point(isPaused = true), point(wasManuallyPaused = true), point(), point()),
                    expectedReduction = 3.0,
                    expectMinConfidence = 0.5,
                ),
            )

        for (case in cases) {
            val result = SmartDistanceAnalyzer.analyze(case.buckets, case.points, odometerDistance = 0.0)
            val option = result.options.single { it.category == case.category }
            assertEquals(case.expectedReduction, option.suggestedReductionKm, absoluteTolerance = 1e-9, message = case.name)
            assertTrue(option.confidence >= case.expectMinConfidence, "${case.name}: confidence ${option.confidence}")
        }
    }

    // -- discrepancy direction -------------------------------------------------------------

    private data class DirectionCase(
        val gps: Double,
        val odo: Double,
        val expected: DiscrepancyDirection,
    )

    @Test
    fun `discrepancy direction reflects which distance is higher`() {
        val cases =
            listOf(
                DirectionCase(gps = 100.0, odo = 80.0, expected = DiscrepancyDirection.GPS_HIGHER),
                DirectionCase(gps = 80.0, odo = 100.0, expected = DiscrepancyDirection.ODOMETER_HIGHER),
                DirectionCase(gps = 50.0, odo = 50.0, expected = DiscrepancyDirection.EQUAL),
            )
        for (case in cases) {
            assertEquals(case.expected, SmartDistanceAnalyzer.discrepancyDirection(case.gps, case.odo), "gps=${case.gps} odo=${case.odo}")
        }
    }

    // -- auto-trigger ------------------------------------------------------------------------

    @Test
    fun `auto-trigger fires above 20 percent discrepancy but not at or below it`() {
        val buckets = emptyBuckets.copy(originalDistance = 125.0)

        val above = SmartDistanceAnalyzer.analyze(buckets, emptyList(), odometerDistance = 100.0)
        assertTrue(above.autoTrigger, "125 vs 100 is a 25% discrepancy, should auto-trigger")

        val atThreshold =
            SmartDistanceAnalyzer.analyze(emptyBuckets.copy(originalDistance = 120.0), emptyList(), odometerDistance = 100.0)
        assertTrue(!atThreshold.autoTrigger, "120 vs 100 is exactly 20%, boundary should not trigger")

        val below =
            SmartDistanceAnalyzer.analyze(emptyBuckets.copy(originalDistance = 105.0), emptyList(), odometerDistance = 100.0)
        assertTrue(!below.autoTrigger, "105 vs 100 is 5%, should not trigger")
    }

    // -- applyReductions -----------------------------------------------------------------------

    @Test
    fun `applyReductions subtracts every selected option and floors at zero`() {
        val options =
            listOf(
                ReductionOption(ReductionCategory.WALKING, suggestedReductionKm = 2.0, confidence = 1.0),
                ReductionOption(ReductionCategory.PAUSE_DRIFT, suggestedReductionKm = 3.0, confidence = 1.0),
            )
        assertEquals(5.0, SmartDistanceAnalyzer.applyReductions(base = 10.0, selected = options))
        assertEquals(0.0, SmartDistanceAnalyzer.applyReductions(base = 4.0, selected = options), "should floor at zero, not go negative")
        assertEquals(10.0, SmartDistanceAnalyzer.applyReductions(base = 10.0, selected = emptyList()), "no selection = no change")
    }

    // -- audit log -------------------------------------------------------------------------------

    @Test
    fun `audit log records exactly the applied reductions`() {
        val options =
            listOf(
                ReductionOption(ReductionCategory.SPEED_OUTLIER, suggestedReductionKm = 1.5, confidence = 0.8),
                ReductionOption(ReductionCategory.BATTERY_OPT, suggestedReductionKm = 0.5, confidence = 0.4),
            )
        val log = SmartDistanceAnalyzer.buildAuditLog(options)
        assertEquals(2, log.size)
        assertEquals(AppliedReduction(ReductionCategory.SPEED_OUTLIER, 1.5), log[0])
        assertEquals(AppliedReduction(ReductionCategory.BATTERY_OPT, 0.5), log[1])
        assertTrue(SmartDistanceAnalyzer.buildAuditLog(emptyList()).isEmpty())
    }

    // -- zero / degenerate inputs -----------------------------------------------------------------

    @Test
    fun `zero points and zero distance are guarded, not divide-by-zero crashes`() {
        val result = SmartDistanceAnalyzer.analyze(emptyBuckets, emptyList(), odometerDistance = 0.0)

        result.options.forEach {
            assertEquals(0.0, it.suggestedReductionKm, "empty input: ${it.category} should suggest no reduction")
            assertEquals(0.0, it.confidence, "empty input: ${it.category} should have zero confidence")
        }
        assertEquals(DiscrepancyDirection.EQUAL, result.discrepancyDirection)
        assertEquals(null, result.discrepancyRatio, "zero odometer distance means ratio is undefined, not Infinity/NaN")
        assertTrue(!result.autoTrigger)
    }

    @Test
    fun `discrepancyRatio is null when odometer distance is zero even with nonzero gps`() {
        assertEquals(null, SmartDistanceAnalyzer.discrepancyRatio(gpsDistance = 50.0, odometerDistance = 0.0))
    }
}
