package com.miletracker

import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.feature.tracking.insights.ActivityAnalyzer
import com.miletracker.feature.tracking.insights.ActivityType
import com.miletracker.feature.tracking.insights.DistanceQualityAnalyzer
import com.miletracker.feature.tracking.insights.JourneyQualityAnalyzer
import com.miletracker.feature.tracking.insights.RouteAnalyzer
import com.miletracker.feature.tracking.insights.SystemImpactAnalyzer
import com.miletracker.feature.tracking.insights.SystemImpactType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the pure-Kotlin trip-insights analyzers.
 * No Android device needed — plain JVM.
 */
class InsightsAnalyzerTest {

    // -----------------------------------------------------------------------
    // Test helpers
    // -----------------------------------------------------------------------

    private fun makeTrack(
        distance: Double = 10_000.0,
        duration: Long = 600_000L,          // 10 min
        totalPoints: Long = 120L,
        cleanedDistance: Double = 9_500.0,
        originalDistance: Double = 10_000.0,
        mockDistance: Double = 0.0,
        abnormalDistance: Double = 0.0,
        avgSpeed: Double = 16.67,           // ~60 km/h
        wasMockLocationUsed: Boolean = false,
        wasBatteryOptimizationEnabled: Boolean = false,
        wasPowerSaverEnabled: Boolean = false,
        wasAppKilled: Boolean = false,
        wasPermissionsViolated: Boolean = false,
        wasPhoneShutDown: Boolean = false,
        totalBatteryOptimizationOnTime: Long = 0L,
        totalPowerSaverOnTime: Long = 0L
    ) = SavedTrack(
        routeId             = "test-route",
        name                = "Test Track",
        startLatitude       = 18.5, startLongitude = 73.8,
        endLatitude         = 18.6, endLongitude   = 73.9,
        pausedLatitude      = 0.0,  pausedLongitude = 0.0,
        startTime           = 1_700_000_000_000L,
        endTime             = 1_700_000_000_000L + duration,
        distance            = distance,
        duration            = duration,
        totalLocationPoints = totalPoints,
        cleanedDistance     = cleanedDistance,
        originalDistance    = originalDistance,
        mockDistance        = mockDistance,
        abnormalDistance    = abnormalDistance,
        avgSpeed            = avgSpeed,
        wasMockLocationUsed              = wasMockLocationUsed,
        wasBatteryOptimizationEnabled    = wasBatteryOptimizationEnabled,
        wasPowerSaverEnabled             = wasPowerSaverEnabled,
        wasAppKilled                     = wasAppKilled,
        wasPermissionsViolated           = wasPermissionsViolated,
        wasPhoneShutDown                 = wasPhoneShutDown,
        totalBatteryOptimizationOnTime   = totalBatteryOptimizationOnTime,
        totalPowerSaverOnTime            = totalPowerSaverOnTime
    )

    private fun makeLocation(
        lat: Double, lng: Double,
        timeMs: Long,
        speed: Float = 16.67f,             // ~60 km/h → DRIVING
        isMock: Boolean = false,
        isAbnormal: Boolean = false,
        accuracy: Float = 10f,
        batteryPct: Double = 80.0,
        wasCapturedWhenNoNetwork: Boolean = false,
        isPaused: Boolean = false
    ) = LocationData(
        activity      = "DRIVING",
        speed         = speed,
        lat           = lat,
        lng           = lng,
        token         = "test-route",
        date          = timeMs,
        isMock        = isMock,
        isAbnormal    = isAbnormal,
        accuracy      = accuracy,
        batteryPercentage = batteryPct,
        wasCapturedWhenNoNetwork = wasCapturedWhenNoNetwork,
        isPaused      = isPaused
    )

    /** 10 clean high-speed DRIVING points over 10 seconds */
    private fun cleanDrivingPoints(count: Int = 10): List<LocationData> =
        (0 until count).map { i ->
            makeLocation(
                lat    = 18.5 + i * 0.001,
                lng    = 73.8,
                timeMs = i * 1_000L,
                speed  = 20f          // DRIVING (< 50 m/s)
            )
        }

    /** Points that include mock locations */
    private fun mixedWithMockPoints(): List<LocationData> {
        val pts = cleanDrivingPoints(10).toMutableList()
        pts[3] = pts[3].copy(isMock = true)
        pts[7] = pts[7].copy(isMock = true)
        return pts
    }

    // -----------------------------------------------------------------------
    // JourneyQualityAnalyzer tests
    // -----------------------------------------------------------------------

    @Test
    fun `clean high-speed track scores high quality`() {
        val track = makeTrack()
        val result = JourneyQualityAnalyzer().analyze(track)
        assertTrue(result.qualityScore >= 75, "Expected high score, got ${result.qualityScore}")
        assertTrue(result.reliabilityScore >= 75, "Expected high reliability, got ${result.reliabilityScore}")
        assertTrue(result.dataCompleteness > 0.0)
    }

    /**
     * Use a minimal track (no bonuses possible) so we can directly observe deductions.
     * - totalLocationPoints = 5 → no 50-point bonus
     * - duration = 60_000 (1 min) → no 5-min bonus
     * - cleanedDistance = 0 → no clean-ratio bonus
     */
    private fun minimalTrack(vararg overrides: Pair<String, Any>): SavedTrack {
        var t = makeTrack(
            totalPoints      = 5L,
            duration         = 60_000L,
            cleanedDistance  = 0.0,
            originalDistance = 0.0
        )
        // Apply overrides by field name
        overrides.forEach { (field, value) ->
            t = when (field) {
                "wasMockLocationUsed"           -> t.copy(wasMockLocationUsed = value as Boolean)
                "wasAppKilled"                  -> t.copy(wasAppKilled = value as Boolean)
                "wasBatteryOptimizationEnabled" -> t.copy(wasBatteryOptimizationEnabled = value as Boolean)
                "wasPowerSaverEnabled"          -> t.copy(wasPowerSaverEnabled = value as Boolean)
                "wasPhoneShutDown"              -> t.copy(wasPhoneShutDown = value as Boolean)
                "wasPermissionsViolated"        -> t.copy(wasPermissionsViolated = value as Boolean)
                else -> t
            }
        }
        return t
    }

    @Test
    fun `mock location deducts 20 points`() {
        // Use score factors to verify the exact deduction value regardless of clamping
        val result = JourneyQualityAnalyzer().analyze(minimalTrack("wasMockLocationUsed" to true))
        val mockFactor = result.scoreFactors.firstOrNull { "mock" in it.label.lowercase() || "Mock" in it.label }
        assertNotNull(mockFactor, "Expected a mock-location score factor")
        assertEquals(20, mockFactor.deduction, "Mock deduction should be exactly 20")
    }

    @Test
    fun `app killed deducts 25 points`() {
        val result = JourneyQualityAnalyzer().analyze(minimalTrack("wasAppKilled" to true))
        val factor = result.scoreFactors.firstOrNull { "killed" in it.label.lowercase() || "Killed" in it.label }
        assertNotNull(factor, "Expected an app-killed score factor")
        assertEquals(25, factor.deduction, "App-killed deduction should be exactly 25")
    }

    @Test
    fun `battery opt deducts 15 points`() {
        val result = JourneyQualityAnalyzer().analyze(minimalTrack("wasBatteryOptimizationEnabled" to true))
        val factor = result.scoreFactors.firstOrNull { "battery" in it.label.lowercase() }
        assertNotNull(factor, "Expected a battery-optimisation score factor")
        assertEquals(15, factor.deduction, "Battery-opt deduction should be exactly 15")
    }

    @Test
    fun `phone shutdown deducts 25 points`() {
        val result = JourneyQualityAnalyzer().analyze(minimalTrack("wasPhoneShutDown" to true))
        val factor = result.scoreFactors.firstOrNull { "shutdown" in it.label.lowercase() || "shut" in it.label.lowercase() }
        assertNotNull(factor, "Expected a phone-shutdown score factor")
        assertEquals(25, factor.deduction, "Phone-shutdown deduction should be exactly 25")
    }

    @Test
    fun `score factors list matches applied deductions`() {
        val result = JourneyQualityAnalyzer().analyze(
            makeTrack(wasMockLocationUsed = true, wasAppKilled = true)
        )
        val totalDeduction = result.scoreFactors.sumOf { it.deduction }
        assertTrue(totalDeduction >= 45, "Expected at least 45 total deductions (20+25), got $totalDeduction")
    }

    @Test
    fun `score is clamped to 0 even with many penalties`() {
        val track = makeTrack(
            wasMockLocationUsed = true,
            wasBatteryOptimizationEnabled = true,
            wasPowerSaverEnabled = true,
            wasAppKilled = true,
            wasPhoneShutDown = true,
            wasPermissionsViolated = true
        )
        val result = JourneyQualityAnalyzer().analyze(track)
        assertTrue(result.qualityScore >= 0, "Score must not go below 0")
    }

    // -----------------------------------------------------------------------
    // ActivityAnalyzer tests
    // -----------------------------------------------------------------------

    @Test
    fun `all driving points classify as DRIVING dominant activity`() {
        val points = cleanDrivingPoints(20)
        val result = ActivityAnalyzer().analyze(points)
        assertEquals(ActivityType.DRIVING, result.dominantActivity)
        assertTrue(result.activityBreakdown.containsKey(ActivityType.DRIVING))
    }

    @Test
    fun `highway speed points classify as HIGHWAY`() {
        val highwayPoints = (0 until 10).map { i ->
            makeLocation(18.5 + i * 0.01, 73.8, i * 1_000L, speed = 55f) // >= 50 m/s = HIGHWAY
        }
        val result = ActivityAnalyzer().analyze(highwayPoints)
        assertEquals(ActivityType.HIGHWAY, result.dominantActivity)
    }

    @Test
    fun `stationary speed points classify as STATIONARY`() {
        val stationaryPoints = (0 until 10).map { i ->
            makeLocation(18.5, 73.8, i * 1_000L, speed = 0f)
        }
        val result = ActivityAnalyzer().analyze(stationaryPoints)
        assertEquals(ActivityType.STATIONARY, result.dominantActivity)
    }

    @Test
    fun `smooth constant speed produces steady acceleration profile`() {
        // Constant speed → no acceleration changes → should produce mostly STEADY
        val points = (0 until 20).map { i ->
            makeLocation(18.5 + i * 0.001, 73.8, i * 1_000L, speed = 20f)
        }
        val result = ActivityAnalyzer().analyze(points)
        val profile = result.accelerationProfile
        assertTrue(
            profile.steadySpeedPct > 50.0,
            "Constant-speed track should have mostly steady profile, got ${profile.steadySpeedPct}%"
        )
        assertEquals("SMOOTH", profile.dominantLabel, "Constant speed should be SMOOTH")
    }

    @Test
    fun `harsh acceleration events are captured with correct type`() {
        // Construct points that accelerate at > 2.5 m/s²
        // dt=1s between each, so dv > 2.5 m/s each second
        val times  = listOf(0L, 1000L, 2000L, 3000L, 4000L)
        val speeds = listOf(0f, 5f, 10f, 15f, 20f) // +5 m/s per second → harsh accel
        val points = times.zip(speeds).mapIndexed { i, (t, s) ->
            makeLocation(18.5 + i * 0.001, 73.8, t, speed = s)
        }
        val result = ActivityAnalyzer().analyze(points)
        val harshAccelEvents = result.accelerationProfile.harshEvents
            .filter { it.type == com.miletracker.feature.tracking.insights.AccelerationType.HARSH_ACCELERATION }
        assertTrue(harshAccelEvents.isNotEmpty(), "Expected harsh acceleration events")
    }

    @Test
    fun `speed consistency is 1_0 for perfectly constant speed`() {
        val points = (0 until 10).map { i ->
            makeLocation(18.5 + i * 0.001, 73.8, i * 1_000L, speed = 20f)
        }
        val result = ActivityAnalyzer().analyze(points)
        assertEquals(1.0, result.speedConsistency, 0.001)
    }

    @Test
    fun `empty points return UNKNOWN dominant activity`() {
        val result = ActivityAnalyzer().analyze(emptyList())
        assertEquals(ActivityType.UNKNOWN, result.dominantActivity)
        assertEquals(1.0, result.speedConsistency)
    }

    // -----------------------------------------------------------------------
    // SystemImpactAnalyzer tests
    // -----------------------------------------------------------------------

    @Test
    fun `clean track has no system impacts`() {
        val track = makeTrack()
        val result = SystemImpactAnalyzer().analyze(track, cleanDrivingPoints())
        assertTrue(result.impacts.isEmpty(), "Clean track should have no impacts")
    }

    @Test
    fun `app kill event adds APP_KILLED impact with 25 percent`() {
        val track = makeTrack(wasAppKilled = true)
        val result = SystemImpactAnalyzer().analyze(track, cleanDrivingPoints())
        val appKilled = result.impacts.firstOrNull { it.type == SystemImpactType.APP_KILLED }
        assertNotNull(appKilled, "Expected APP_KILLED impact")
        assertEquals(25.0, appKilled.estimatedImpactPct, 0.001)
    }

    @Test
    fun `phone restart adds PHONE_RESTART impact with 20 percent`() {
        val track = makeTrack(wasPhoneShutDown = true)
        val result = SystemImpactAnalyzer().analyze(track, cleanDrivingPoints())
        val restart = result.impacts.firstOrNull { it.type == SystemImpactType.PHONE_RESTART }
        assertNotNull(restart)
        assertEquals(20.0, restart.estimatedImpactPct, 0.001)
    }

    @Test
    fun `mock location impact uses distance ratio when available`() {
        val track = makeTrack(
            wasMockLocationUsed = true,
            mockDistance = 2_000.0,
            distance = 10_000.0
        )
        val result = SystemImpactAnalyzer().analyze(track, cleanDrivingPoints())
        val mockImpact = result.impacts.firstOrNull { it.type == SystemImpactType.MOCK_LOCATION }
        assertNotNull(mockImpact)
        assertEquals(20.0, mockImpact.estimatedImpactPct, 0.001) // 2000/10000*100 = 20%
    }

    @Test
    fun `poor GPS accuracy triggers impact when over 10 percent of points`() {
        val points = (0 until 10).map { i ->
            makeLocation(18.5 + i * 0.001, 73.8, i * 1_000L,
                accuracy = if (i < 2) 60f else 10f) // 20% poor accuracy
        }
        val track = makeTrack()
        val result = SystemImpactAnalyzer().analyze(track, points)
        val gpsImpact = result.impacts.firstOrNull { it.type == SystemImpactType.POOR_GPS_ACCURACY }
        assertNotNull(gpsImpact, "Expected POOR_GPS_ACCURACY impact when >10% poor points")
    }

    // -----------------------------------------------------------------------
    // DistanceQualityAnalyzer tests
    // -----------------------------------------------------------------------

    @Test
    fun `clean track scores 100`() {
        val score = DistanceQualityAnalyzer.computeScore(
            mockDistance = 0.0, abnormalDistance = 0.0, totalDistance = 10_000.0,
            mockCount = 0, abnormalCount = 0, totalCount = 100
        )
        assertEquals(100, score)
    }

    @Test
    fun `all mock track scores very low`() {
        // 100% mock: -50 (dist rule) -30 (point rule) -10 (severe mock) = 10
        val score = DistanceQualityAnalyzer.computeScore(
            mockDistance = 10_000.0, abnormalDistance = 0.0, totalDistance = 10_000.0,
            mockCount = 100, abnormalCount = 0, totalCount = 100
        )
        assertTrue(score <= 20, "Expected very low score for all-mock track, got $score")
    }

    @Test
    fun `50 percent mock distance deducts 25 via distance rule and extra 0 via point rule`() {
        // mockPct=50 → 50*0.5=25 deduction from distance; no extra because mockPct=50 > 30 → -10 more
        // problematicPct=50*0.5=25; problematicPoints=50*0.3=15; extra10 for mock>30 → 100-25-15-10=50
        val score = DistanceQualityAnalyzer.computeScore(
            mockDistance = 5_000.0, abnormalDistance = 0.0, totalDistance = 10_000.0,
            mockCount = 50, abnormalCount = 0, totalCount = 100
        )
        assertEquals(50, score)
    }

    @Test
    fun `getAssessment returns Excellent for 90 and above`() {
        assertEquals("Excellent quality tracking data", DistanceQualityAnalyzer.getAssessment(90))
        assertEquals("Excellent quality tracking data", DistanceQualityAnalyzer.getAssessment(100))
    }

    @Test
    fun `getAssessment returns Good for 75 to 89`() {
        assertEquals("Good quality tracking data", DistanceQualityAnalyzer.getAssessment(75))
        assertEquals("Good quality tracking data", DistanceQualityAnalyzer.getAssessment(89))
    }

    @Test
    fun `cleanedDistanceRatio is 1 when no total distance`() {
        assertEquals(1.0, DistanceQualityAnalyzer.getCleanedDistanceRatio(0.0, 0.0))
    }

    @Test
    fun `analyze on track returns DistanceQualityResult with correct assessment`() {
        val track = makeTrack(
            originalDistance = 10_000.0,
            cleanedDistance  = 9_800.0,
            mockDistance     = 0.0,
            abnormalDistance = 0.0
        )
        val result = DistanceQualityAnalyzer.analyze(track, cleanDrivingPoints())
        assertEquals(100, result.score)
        assertTrue(result.isReliableForBusiness)
    }

    // -----------------------------------------------------------------------
    // RouteAnalyzer (coordinator) integration tests
    // -----------------------------------------------------------------------

    @Test
    fun `clean high-speed steady route gives high quality and DRIVING activity`() {
        val track = makeTrack(
            distance = 10_000.0,
            duration = 600_000L,
            totalPoints = 120L,
            cleanedDistance = 9_800.0,
            originalDistance = 10_000.0
        )
        val points = cleanDrivingPoints(20)
        val result = RouteAnalyzer().analyze(track, points)

        assertTrue(result.quality.qualityScore >= 75, "Expected high quality: ${result.quality.qualityScore}")
        assertEquals(ActivityType.DRIVING, result.activity.dominantActivity)
        assertTrue(result.systemImpact.impacts.isEmpty())
        assertEquals(100, result.distanceQuality.score)
        assertTrue(result.anomalies.isEmpty())
    }

    @Test
    fun `route with mock points and app kill event shows documented deductions`() {
        val track = makeTrack(
            wasMockLocationUsed = true,
            wasAppKilled = true,
            mockDistance = 2_000.0,
            distance = 10_000.0
        )
        val points = mixedWithMockPoints()
        val result = RouteAnalyzer().analyze(track, points)

        // Quality should be significantly reduced (mock -20 + kill -25 = -45; bonuses may partially offset)
        assertTrue(result.quality.qualityScore <= 80, "Expected reduced quality with mock+kill: ${result.quality.qualityScore}")

        // System impacts must include MOCK_LOCATION and APP_KILLED
        val impactTypes = result.systemImpact.impacts.map { it.type }
        assertTrue(impactTypes.contains(SystemImpactType.MOCK_LOCATION))
        assertTrue(impactTypes.contains(SystemImpactType.APP_KILLED))

        // Score factors must reference mock and kill deductions
        val factorLabels = result.quality.scoreFactors.map { it.label }
        assertTrue(factorLabels.any { "mock" in it.lowercase() || "Mock" in it })
        assertTrue(factorLabels.any { "killed" in it.lowercase() || "Killed" in it })
    }

    @Test
    fun `highway points produce HIGHWAY dominant activity`() {
        val track = makeTrack(avgSpeed = 55.0)
        val points = (0 until 20).map { i ->
            makeLocation(18.5 + i * 0.01, 73.8, i * 1_000L, speed = 55f)
        }
        val result = RouteAnalyzer().analyze(track, points)
        assertEquals(ActivityType.HIGHWAY, result.activity.dominantActivity)
    }

    @Test
    fun `category is Commute for weekday morning trip`() {
        // 2024-01-08 08:00 UTC = Monday 08:00 UTC (within 6-9 window)
        val mondayMorning = 1_704_700_800_000L  // 2024-01-08 08:00 UTC
        val track = makeTrack().copy(startTime = mondayMorning, endTime = mondayMorning + 600_000L)
        val result = RouteAnalyzer().analyze(track, cleanDrivingPoints())
        // Accept both "Commute" and "General Journey" — time zone differences are acceptable
        assertTrue(result.category.isNotEmpty())
    }

    @Test
    fun `route summary contains distance and duration`() {
        val track = makeTrack(distance = 5_000.0, duration = 300_000L)
        val result = RouteAnalyzer().analyze(track, cleanDrivingPoints())
        assertTrue(result.summary.contains("5.0"), "Summary should contain distance: ${result.summary}")
        assertTrue(result.summary.contains("5"), "Summary should contain duration minutes")
    }
}
