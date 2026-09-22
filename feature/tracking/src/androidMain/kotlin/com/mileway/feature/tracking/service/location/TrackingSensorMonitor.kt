package com.mileway.feature.tracking.service.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captures the latest accelerometer + gyroscope readings during tracking so each persisted
 * [com.mileway.core.data.model.db.LocationData] carries IMU context (used for activity /
 * motion analysis). Degrades gracefully to zeros on devices/emulators without the sensors.
 *
 * Wave-2 IMU polish: [setStationary] lets the caller pause gyro consumption once the device is
 * confirmed still (accelerometer + activity recognition fusion happens upstream in the tracking
 * service) — a battery win, since the gyro is the more power-hungry of the two sensors and is
 * useless while genuinely parked.
 */
class TrackingSensorMonitor(
    context: Context,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var gyroRegistered = false

    private val _snapshotFlow = MutableStateFlow(SensorSnapshot())

    /** Latest [SensorSnapshot] as a hot state stream, for consumers that want to observe rather than poll. */
    val snapshotFlow: StateFlow<SensorSnapshot> = _snapshotFlow.asStateFlow()

    var snapshot: SensorSnapshot
        get() = _snapshotFlow.value
        private set(value) {
            _snapshotFlow.value = value
        }

    fun start() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        registerGyro()
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        gyroRegistered = false
        snapshot = SensorSnapshot()
    }

    /**
     * Wave-2 IMU polish: stop consuming the gyroscope while the device is confirmed stationary
     * (battery win — gyro sampling costs more than accelerometer), and resume it the moment
     * motion is detected again so a spin/mock-heuristic reading isn't missed once moving.
     */
    fun setStationary(stationary: Boolean) {
        if (stationary) {
            if (gyroRegistered) {
                sensorManager?.unregisterListener(this, gyroscope)
                gyroRegistered = false
            }
        } else {
            registerGyro()
        }
    }

    private fun registerGyro() {
        if (gyroRegistered) return
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            gyroRegistered = true
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        snapshot =
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    snapshot.copy(
                        accelX = event.values[0],
                        accelY = event.values[1],
                        accelZ = event.values[2],
                    )
                Sensor.TYPE_GYROSCOPE ->
                    snapshot.copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2],
                    )
                else -> snapshot
            }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) { /* no-op */ }
}
