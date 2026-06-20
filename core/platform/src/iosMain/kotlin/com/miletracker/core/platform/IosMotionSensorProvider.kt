package com.miletracker.core.platform

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

    @OptIn(ExperimentalForeignApi::class)
    override fun start() {
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
        manager.stopDeviceMotionUpdates()
    }
}
