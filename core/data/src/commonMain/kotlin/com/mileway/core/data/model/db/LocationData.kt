package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index(value = ["token", "uploaded", "date"])],
)
data class LocationData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activity: String,
    val speed: Float,
    val lat: Double,
    val lng: Double,
    val token: String,
    val date: Long = 0L,
    val uploaded: Boolean = false,
    val displacement: Double = 0.0,
    val accuracy: Float = 0f,
    val isMock: Boolean = false,
    val isAbnormal: Boolean = false,
    val wasCapturedWhenNoNetwork: Boolean = false,
    val wasAppKilled: Boolean = false,
    val wasPhoneShutdown: Boolean = false,
    val inActivityTime: Long = 0,
    val isPaused: Boolean = false,
    val wasManuallyPaused: Boolean = false,
    val reason: String? = "",
    val wasBatteryOptimizationEnabled: Boolean = false,
    val batteryPercentage: Double,
    val wasPowerSaverModeEnabled: Boolean = false,
    val gyroscopeX: Float = 0f,
    val gyroscopeY: Float = 0f,
    val gyroscopeZ: Float = 0f,
    val accelerometerX: Float = 0f,
    val accelerometerY: Float = 0f,
    val accelerometerZ: Float = 0f,
    val wasCheckInPoint: Boolean = false,
    val checkInType: String = "NONE",
    val miscellaneous: String = "",
    val attachments: String = "",
    val provider: String = "NONE",
    val bearing: Float = 0f,
    val bearingAccuracyDegrees: Float = 0f,
    val elapsedRealtimeNanos: Long = 0L,
    val mslAltitudeAccuracyMeters: Float = 0f,
    val elapsedRealtimeAgeMillis: Long = 0L,
    val speedAccuracyMetersPerSecond: Float = 0f,
    val verticalAccuracyMeters: Float = 0f,
    val altitude: Double = 0.0,
    val locationTime: Long = 0L,
    val ramUsage: Long = 0L,
    val deviceModel: String = "",
    val appVersionName: String = "",
)
