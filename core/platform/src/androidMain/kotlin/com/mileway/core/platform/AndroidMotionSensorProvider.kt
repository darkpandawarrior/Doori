package com.mileway.core.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android motion sensors (O) via SensorManager, accelerometer + gyroscope merged into one [MotionReading]
 * stream. Each fresh accelerometer sample emits a reading carrying the last-seen gyro axes (and vice-versa),
 * so downstream [MotionFusion] always has both. Degrades to no emissions on devices without the sensors.
 */
class AndroidMotionSensorProvider(context: Context) : MotionSensorProvider, SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _readings = MutableSharedFlow<MotionReading>(replay = 1, extraBufferCapacity = 8)
    override val readings: Flow<MotionReading> = _readings.asSharedFlow()

    @Volatile
    private var last = MotionReading()

    override fun start() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun stop() {
        sensorManager?.unregisterListener(this)
        last = MotionReading()
    }

    override fun onSensorChanged(event: SensorEvent) {
        last =
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    last.copy(
                        accelX = event.values[0],
                        accelY = event.values[1],
                        accelZ = event.values[2],
                        timestampMillis = System.currentTimeMillis(),
                    )
                Sensor.TYPE_GYROSCOPE ->
                    last.copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2],
                        timestampMillis = System.currentTimeMillis(),
                    )
                else -> last
            }
        _readings.tryEmit(last)
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) { /* no-op */ }
}
