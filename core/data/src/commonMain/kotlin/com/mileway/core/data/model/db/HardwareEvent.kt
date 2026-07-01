package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hardware_events",
    indices = [Index(value = ["time"], orders = [Index.Order.ASC])],
)
data class HardwareEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val token: String,
    val eventType: EventType = EventType.UNKNOWN,
    val tag: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val speed: Float? = null,
    val event: String,
    val uploaded: Boolean = false,
    val network: Boolean = false,
    val disp: Double? = null,
    val time: Long = 0L,
    val activity: String? = null,
    val metadata: String? = null,
    val battery: Double? = null,
    val debugDetails: String? = null,
    val audience: EventAudience = EventAudience.UNKNOWN,
    val deviceModel: String = "",
    val appVersionName: String = "",
) {
    companion object {
        fun inferEventType(eventText: String): EventType =
            when {
                eventText.contains("Started", ignoreCase = true) -> EventType.TRACKING_STARTED
                eventText.contains("Stopped", ignoreCase = true) || eventText.contains("Ended", ignoreCase = true) -> EventType.TRACKING_STOPPED
                eventText.contains("Paused", ignoreCase = true) -> EventType.TRACKING_PAUSED
                eventText.contains("Resumed", ignoreCase = true) -> EventType.TRACKING_RESUMED
                eventText.contains("Check", ignoreCase = true) && eventText.contains("In", ignoreCase = true) -> EventType.CHECK_IN
                eventText.contains("Mock", ignoreCase = true) -> EventType.MOCK_LOCATION
                eventText.contains("Abnormal", ignoreCase = true) -> EventType.ABNORMAL_LOCATION
                eventText.contains("App Killed", ignoreCase = true) -> EventType.APP_KILLED
                eventText.contains("Phone Restart", ignoreCase = true) -> EventType.PHONE_RESTART
                eventText.contains("Battery Opt", ignoreCase = true) -> EventType.BATTERY_OPTIMIZATION_ON
                eventText.contains("Power Saver", ignoreCase = true) -> EventType.POWER_SAVER_ON
                eventText.contains("GPS", ignoreCase = true) && eventText.contains("Lost", ignoreCase = true) -> EventType.GPS_LOST
                eventText.contains("GPS", ignoreCase = true) && eventText.contains("Available", ignoreCase = true) -> EventType.GPS_REGAINED
                eventText.contains("Logout", ignoreCase = true) -> EventType.LOGOUT_DETECTED
                eventText.contains("Odometer Start", ignoreCase = true) -> EventType.ODOMETER_START_CAPTURED
                eventText.contains("Odometer End", ignoreCase = true) -> EventType.ODOMETER_END_CAPTURED
                else -> EventType.CUSTOM
            }
    }
}
