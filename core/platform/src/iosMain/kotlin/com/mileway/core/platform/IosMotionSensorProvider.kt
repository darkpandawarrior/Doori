package com.mileway.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * iOS motion sensors (O) via CoreMotion. Uses device-motion updates (gravity-compensated userAcceleration +
 * rotationRate) at ~30 Hz on the main queue, mapped to the shared [MotionReading] stream. Compiles + links
 * against the simulator framework; live data needs a device.
 */
class IosMotionSensorProvider : MotionSensorProvider {
    private val manager = CMMotionManager()
    private val _readings = MutableSharedFlow<MotionReading>(replay = 1, extraBufferCapacity = 8)
    override val readings: Flow<MotionReading> = _readings.asSharedFlow()

    /**
     * Outstanding [start] calls — see [MotionSensorProvider.start]. Not atomic because CoreMotion
     * delivers on [NSOperationQueue.mainQueue] and every caller on iOS drives this from the main thread;
     * the Android actual uses an AtomicInteger because its callers are genuinely concurrent.
     */
    private var activeStarts = 0

    @OptIn(ExperimentalForeignApi::class)
    override fun start() {
        if (activeStarts++ != 0) return
        if (!manager.deviceMotionAvailable) return
        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            if (motion == null) return@startDeviceMotionUpdatesToQueue
            val accel = motion.userAcceleration.useContents { Triple(x, y, z) }
            val gyro = motion.rotationRate.useContents { Triple(x, y, z) }
            _readings.tryEmit(
                MotionReading(
                    accelX = accel.first.toFloat(),
                    accelY = accel.second.toFloat(),
                    accelZ = accel.third.toFloat(),
                    gyroX = gyro.first.toFloat(),
                    gyroY = gyro.second.toFloat(),
                    gyroZ = gyro.third.toFloat(),
                    timestampMillis = 0L,
                ),
            )
        }
    }

    override fun stop() {
        // Clamp at 0 so a stray unpaired stop() can't wedge the next start().
        activeStarts = (activeStarts - 1).coerceAtLeast(0)
        if (activeStarts > 0) return
        manager.stopDeviceMotionUpdates()
    }
}
