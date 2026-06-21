package com.miletracker

import com.miletracker.feature.tracking.service.location.QualityInputs
import com.miletracker.feature.tracking.service.location.TrackingQualityScorer
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [TrackingQualityScorer]. Pure Kotlin — no device needed.
 */
class TrackingQualityScorerTest {

    @Test
    fun `perfect conditions score 100`() {
        assertEquals(100, TrackingQualityScorer.score(QualityInputs(accuracyM = 8f)))
    }

    @Test
    fun `stable bonus cannot exceed the max`() {
        assertEquals(100, TrackingQualityScorer.score(QualityInputs(accuracyM = 8f, isStable = true)))
    }

    @Test
    fun `mock location deducts 25`() {
        assertEquals(75, TrackingQualityScorer.score(QualityInputs(isMock = true, accuracyM = 8f)))
    }

    @Test
    fun `missing permission deducts 30`() {
        assertEquals(70, TrackingQualityScorer.score(QualityInputs(isPermissionMissing = true, accuracyM = 8f)))
    }

    @Test
    fun `accuracy tiers deduct progressively`() {
        assertEquals(100, TrackingQualityScorer.score(QualityInputs(accuracyM = 15f))) // excellent
        assertEquals(95, TrackingQualityScorer.score(QualityInputs(accuracyM = 30f))) // good -5
        assertEquals(90, TrackingQualityScorer.score(QualityInputs(accuracyM = 60f))) // fair -10
        assertEquals(80, TrackingQualityScorer.score(QualityInputs(accuracyM = 120f))) // poor -20
    }

    @Test
    fun `stable bonus offsets a single deduction`() {
        // mock -25 + stable +5 = 80
        assertEquals(80, TrackingQualityScorer.score(QualityInputs(isMock = true, accuracyM = 8f, isStable = true)))
    }

    @Test
    fun `deductions stack`() {
        // power-saver -15, battery-opt -15, poor accuracy -20 → 50
        val s = TrackingQualityScorer.score(
            QualityInputs(isPowerSaver = true, isBatteryOptimized = true, accuracyM = 120f),
        )
        assertEquals(50, s)
    }

    @Test
    fun `worst case clamps to zero`() {
        val s = TrackingQualityScorer.score(
            QualityInputs(
                isMock = true, // -25
                isBatteryOptimized = true, // -15
                isPowerSaver = true, // -15
                wasAppKilled = true, // -20
                wasRestarted = true, // -20
                isPermissionMissing = true, // -30
                isGpsOff = true, // -20
                accuracyM = 200f, // -20
            ),
        )
        assertEquals(0, s)
    }
}
