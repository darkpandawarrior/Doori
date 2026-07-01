package com.mileway

import com.mileway.core.platform.MotionReading
import com.mileway.core.platform.MotionState
import com.mileway.core.platform.toMotionState
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O.1: the pure IMU → [MotionState] derivation. Once the gravity filter converges on a steady 1g reading
 * it reports STILL; a real lateral acceleration flips it to MOVING. Emits only on change.
 */
class MotionStateTest {
    @Test
    fun `settles to STILL under steady gravity, then flags MOVING on real acceleration`() =
        runTest {
            val still = (0 until 20).map { MotionReading(accelX = 0f, accelY = 0f, accelZ = 9.8f, timestampMillis = it * 100L) }
            val moving = MotionReading(accelX = 6f, accelY = 0f, accelZ = 9.8f, timestampMillis = 2_000L)

            val states = (still + moving).asFlow().toMotionState().toList()

            assertTrue("should report STILL once gravity converges", states.contains(MotionState.STILL))
            assertEquals("a strong lateral accel is MOVING", MotionState.MOVING, states.last())
        }
}
