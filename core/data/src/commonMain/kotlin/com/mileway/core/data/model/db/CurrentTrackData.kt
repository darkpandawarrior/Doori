package com.mileway.core.data.model.db

import com.mileway.core.data.util.fmt1d
import com.mileway.core.data.util.fmt2d

data class CurrentTrackData(
    val token: String,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val pausedLatitude: Double = 0.0,
    val pausedLongitude: Double = 0.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val distance: Double = 0.0,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val minimumTrackerDistance: Double = 10.0,
    val minimumTrackerTime: Long = 10_000L,
    val maximumTrackerTime: Long = 10_000L,
    val pauseReason: String? = null,
    val selectedVehicleType: String = "",
    val vehiclePricing: Double = 0.0,
    val service: String = "",
    val trackingActivity: String = "Walking",
    val avgSpeed: Double = 0.0,
    val speed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val lastSyncedTime: Long = -1L,
    val totalLocationPoints: Long = 0L,
    val unsyncedLocationPoints: Long = 0L,
    val wasEverPaused: Boolean = false,
    val wasMockOn: Boolean = false,
    val wasBatteryOptimizationEnabled: Boolean = false,
    val wasPowerSaverEnabled: Boolean = false,
    val lastHardwareEventText: String = "",
    val lastHardwareEventTime: Long = -1L,
    val startedByEmployeeCode: String = "",
    val startedByAccountEmail: String = "demo@mileway.app",
    val startedByTenant: String = "DEMO",
    val startedAtTimestamp: Long = 0L,
) {
    fun isEmpty(): Boolean = token.isEmpty()

    fun getFormattedDistance(): String = "${(distance / 1000.0).fmt2d()} km"

    fun getFormattedSpeed(): String = "${(speed * 3.6).fmt1d()} km/h"

    companion object {
        fun empty() = CurrentTrackData(token = "")
    }
}
