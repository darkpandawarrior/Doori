package com.mileway.feature.tracking.service.location

import com.siddharth.kmp.location.algo.AlgorithmId
import com.siddharth.kmp.location.algo.SegmentedTripAlgorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShadowAlgorithmRunnerTest {

    private fun drive(points: Int, stepDeg: Double = 0.0005, intervalMs: Long = 5_000L) =
        (0 until points).map { i ->
            GpsFix(
                lat = 12.9 + i * stepDeg,
                lng = 77.6,
                timeMs = 1_000L + i * intervalMs,
                speedMps = 11f,
                accuracyM = 6f,
            )
        }

    @Test
    fun disabled_by_default_so_shipping_it_changes_nothing() {
        val runner = ShadowAlgorithmRunner()
        runner.reset(0L)
        drive(20).forEach(runner::onFix)
        assertTrue(runner.finish().isEmpty(), "a disabled runner must produce nothing at all")
    }

    @Test
    fun enabled_runner_reports_one_result_per_candidate() {
        val runner = ShadowAlgorithmRunner(enabled = true)
        runner.reset(1_000L)
        drive(30).forEach(runner::onFix)
        val results = runner.finish()

        assertEquals(2, results.size)
        assertTrue(results.any { it.algorithmId == AlgorithmId.TieredGps })
        assertTrue(results.any { it.algorithmId == SegmentedTripAlgorithm.Id })
        assertTrue(results.all { it.cleanedKm > 0.0 }, "a real drive should accumulate distance")
    }

    @Test
    fun candidates_accumulate_independently() {
        val runner = ShadowAlgorithmRunner(enabled = true)
        runner.reset(1_000L)
        drive(30).forEach(runner::onFix)
        val byId = runner.finish().associateBy { it.algorithmId }

        // Both see the same trace, so neither may be empty; they are allowed to disagree on the
        // total, which is the entire point of running them side by side.
        assertTrue((byId[AlgorithmId.TieredGps]?.cleanedKm ?: 0.0) > 0.0)
        assertTrue((byId[SegmentedTripAlgorithm.Id]?.cleanedKm ?: 0.0) > 0.0)
    }

    @Test
    fun delta_is_reported_against_the_live_figure_without_judging_it() {
        val runner = ShadowAlgorithmRunner(enabled = true)
        runner.reset(1_000L)
        drive(30).forEach(runner::onFix)
        val r = runner.finish().first()

        assertEquals(r.cleanedKm - 1.0, r.deltaKmVersus(1.0), absoluteTolerance = 1e-9)
    }

    @Test
    fun fixes_before_reset_are_ignored_rather_than_attributed_to_nothing() {
        val runner = ShadowAlgorithmRunner(enabled = true)
        drive(10).forEach(runner::onFix) // no reset() yet
        assertTrue(runner.finish().isEmpty())
    }

    @Test
    fun a_zero_speed_maps_to_absent_not_to_stationary() {
        // GpsFix defaults speedMps to 0f, so absence and standstill are indistinguishable here.
        // Claiming a measurement that was never taken is the worse error.
        val fix = GpsFix(lat = 12.9, lng = 77.6, timeMs = 1L, accuracyM = 5f)
        assertNull(fix.toAlgoFix().speedMps)

        val moving = fix.copy(speedMps = 12.5f)
        assertEquals(12.5, moving.toAlgoFix().speedMps!!, absoluteTolerance = 1e-6)
    }

    @Test
    fun doppler_speed_and_mock_flag_survive_the_mapping() {
        // Doppler speed is what SegmentedTripAlgorithm prefers over position-differenced speed;
        // dropping it here would silently disable that behaviour.
        val fix = GpsFix(lat = 1.0, lng = 2.0, timeMs = 5L, speedMps = 8f, accuracyM = 7f, isMock = true)
        val mapped = fix.toAlgoFix()

        assertEquals(8.0, mapped.speedMps!!, absoluteTolerance = 1e-6)
        assertEquals(7.0, mapped.accuracyM, absoluteTolerance = 1e-6)
        assertTrue(mapped.isMock)
    }

    @Test
    fun finish_is_idempotent_and_does_not_double_report() {
        val runner = ShadowAlgorithmRunner(enabled = true)
        runner.reset(1_000L)
        drive(15).forEach(runner::onFix)

        assertEquals(2, runner.finish().size)
        assertTrue(runner.finish().isEmpty(), "a second finish must not report the journey again")
    }
}
