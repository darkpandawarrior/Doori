package com.mileway

import com.mileway.core.platform.MotionReading
import com.mileway.core.platform.MotionSensorProvider
import com.mileway.feature.tracking.detection.DetectedDriveRecorder
import com.mileway.feature.tracking.detection.PendingDrive
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HeuristicDriveStartSourceTest {
    @Test
    fun waitDoesNotStartForGravityWarmupAndStartsOnceOnMovement() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val sensors = FakeSensors()
                val recorder = mockk<DetectedDriveRecorder>()
                coEvery { recorder.start(any()) } returns Unit
                val source = HeuristicDriveStartSource(sensors, recorder)
                val drive = PendingDrive("trip", "Journey", "car", 7.0, "employee", "account", "tenant")
                assertTrue(source.arm(drive))
                repeat(20) { sensors.readings.emit(MotionReading(accelZ = 9.8f)) }
                assertTrue(source.waiting.value)
                coVerify(exactly = 0) { recorder.start(any()) }
                sensors.readings.emit(MotionReading(accelX = 6f, accelZ = 9.8f))
                sensors.readings.emit(MotionReading(accelX = 8f, accelZ = 9.8f))
                coVerify(exactly = 1) { recorder.start(drive) }
                assertFalse(source.waiting.value)
                assertEquals(1, sensors.starts)
                assertEquals(1, sensors.stops)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun cancelWaitReleasesSensorsWithoutStartingRecording() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val sensors = FakeSensors()
                val recorder = mockk<DetectedDriveRecorder>()
                val source = HeuristicDriveStartSource(sensors, recorder)
                source.arm(PendingDrive("trip", "Journey", "car", 7.0, "employee", "account", "tenant"))
                source.disarm()
                assertFalse(source.waiting.value)
                assertEquals(sensors.starts, sensors.stops)
                coVerify(exactly = 0) { recorder.start(any()) }
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class FakeSensors : MotionSensorProvider {
        override val readings = MutableSharedFlow<MotionReading>()
        var starts = 0
        var stops = 0

        override fun start() {
            starts++
        }

        override fun stop() {
            stops++
        }
    }
}
