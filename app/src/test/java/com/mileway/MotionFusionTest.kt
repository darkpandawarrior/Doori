package com.mileway

import com.mileway.core.platform.MotionFusion
import com.mileway.core.platform.MotionReading
import com.mileway.core.platform.Vector3
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O: MotionFusion is pure (no platform types), so the gravity low-pass filter, linear-acceleration
 * subtraction, and movement threshold are verified directly.
 */
class MotionFusionTest {

    @Test
    fun `gravity estimate converges toward a steady 1g on the z axis`() {
        val steady = MotionReading(accelX = 0f, accelY = 0f, accelZ = 9.81f)
        var gravity = Vector3(0f, 0f, 0f)
        repeat(200) { gravity = MotionFusion.updateGravity(gravity, steady) }
        assertTrue(gravity.z in 9.5f..9.81f, "z gravity should converge near 9.81 but was ${gravity.z}")
        assertEquals(0f, gravity.x, 0.01f)
    }

    @Test
    fun `linear acceleration removes the gravity component`() {
        val gravity = Vector3(0f, 0f, 9.81f)
        val reading = MotionReading(accelX = 0f, accelY = 0f, accelZ = 9.81f)
        val linear = MotionFusion.linearAcceleration(reading, gravity)
        assertEquals(0f, linear.magnitude, 0.001f)
    }

    @Test
    fun `isMoving is false at rest and true under a strong push`() {
        val gravity = Vector3(0f, 0f, 9.81f)
        val atRest = MotionReading(accelZ = 9.81f)
        val pushed = MotionReading(accelX = 2.0f, accelZ = 9.81f)
        assertFalse(MotionFusion.isMoving(atRest, gravity))
        assertTrue(MotionFusion.isMoving(pushed, gravity))
    }
}
