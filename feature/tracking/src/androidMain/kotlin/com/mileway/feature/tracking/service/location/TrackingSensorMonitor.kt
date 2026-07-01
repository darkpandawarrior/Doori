package com.mileway.feature.tracking.service.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Captures the latest accelerometer + gyroscope readings during tracking so each persisted
 * [com.mileway.core.data.model.db.LocationData] carries IMU context (used for activity /
 * motion analysis). Degrades gracefully to zeros on devices/emulators without the sensors.
 */
class TrackingSensorMonitor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile
    var snapshot: SensorSnapshot = SensorSnapshot()
        private set

    fun start() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        snapshot = SensorSnapshot()
    }

    override fun onSensorChanged(event: SensorEvent) {
        snapshot =
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    snapshot.copy(
                        accelX = event.values[0], accelY = event.values[1], accelZ = event.values[2],
                    )
                Sensor.TYPE_GYROSCOPE ->
                    snapshot.copy(
                        gyroX = event.values[0], gyroY = event.values[1], gyroZ = event.values[2],
                    )
                else -> snapshot
            }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) { /* no-op */ }
}
