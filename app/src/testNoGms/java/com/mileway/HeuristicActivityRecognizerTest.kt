package com.mileway

import com.mileway.core.platform.MotionReading
import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.MotionState
import com.mileway.feature.tracking.service.location.RecognizedActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN_V37 Phase 1: the FOSS flavor's IMU-backed [HeuristicActivityRecognizer]. Covers the pure
 * [MotionActivityMapper] contract (movement is never claimed to be a travel mode) and the flow wiring —
 * the recognizer must emit a *changing* signal and own the sensor's start/stop.
 */
class HeuristicActivityRecognizerTest {
    private class FakeMotionSensorProvider(samples: List<MotionReading>) : MotionSensorProvider {
        var started = false
        var stopped = false

        override val readings: Flow<MotionReading> = samples.asFlow()

        override fun start() {
            started = true
        }

        override fun stop() {
            stopped = true
        }
    }

    @Test
    fun `only stillness is asserted, movement stays unknown`() {
        assertEquals(RecognizedActivity.STILL, MotionActivityMapper.fromMotionState(MotionState.STILL))
        assertEquals(RecognizedActivity.UNKNOWN, MotionActivityMapper.fromMotionState(MotionState.MOVING))
        assertEquals(RecognizedActivity.UNKNOWN, MotionActivityMapper.fromMotionState(MotionState.UNKNOWN))
    }

    @Test
    fun `emits a changing signal as the IMU settles and then moves`() =
        runTest {
            val atRest = (0 until 20).map { MotionReading(accelZ = 9.8f, timestampMillis = it * 100L) }
            val pushed = MotionReading(accelX = 6f, accelZ = 9.8f, timestampMillis = 2_000L)
            val provider = FakeMotionSensorProvider(atRest + pushed)

            val activities = HeuristicActivityRecognizer(provider).activity.toList()

            // Gravity starts at zero, so the first sample reads as movement until the filter converges.
            assertEquals(
                listOf(RecognizedActivity.UNKNOWN, RecognizedActivity.STILL, RecognizedActivity.UNKNOWN),
                activities,
            )
        }

    @Test
    fun `collection owns the shared sensor's start and stop`() =
        runTest {
            val provider = FakeMotionSensorProvider(listOf(MotionReading(accelZ = 9.8f)))

            HeuristicActivityRecognizer(provider).activity.toList()

            assertTrue("sensor registered on collect", provider.started)
            assertTrue("sensor unregistered when collection ends", provider.stopped)
        }
}
