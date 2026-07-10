package com.mileway.core.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_tracks",
    indices = [
        Index(value = ["started_by_account_id"]),
        Index(value = ["started_by_employee_code"]),
        Index(value = ["isCompleted"]),
        Index(value = ["createdAt"]),
    ],
)
data class SavedTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: String,
    val name: String,
    val isCompleted: Boolean = false,
    val isDraft: Boolean = false,
    val draftSavedAt: Long = 0L,
    val isRetained: Boolean = false,
    val isDiscarded: Boolean = false,
    @ColumnInfo(name = "started_by_account_id")
    val startedByAccountId: String? = null,
    @ColumnInfo(name = "started_by_employee_code")
    val startedByEmployeeCode: String = "",
    @ColumnInfo(name = "started_by_account_email")
    val startedByAccountEmail: String = "",
    @ColumnInfo(name = "started_by_tenant")
    val startedByTenant: String = "",
    @ColumnInfo(name = "started_at_timestamp")
    val startedAtTimestamp: Long = 0L,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val pausedLatitude: Double,
    val pausedLongitude: Double,
    val startTime: Long,
    val endTime: Long,
    val distance: Double,
    val duration: Long,
    val minimumTrackerDistance: Double = 10.0,
    val minimumTrackerTime: Long = 10_000L,
    val maximumTrackerTime: Long = 10_000L,
    val createdAt: Long = 0L,
    val pettyId: Long = -1L,
    val selectedVehicleType: String = "NONE",
    val vehiclePricing: Double = -1.0,
    val service: String = "OWN CAR INTRA",
    val trackingActivity: String = "Journey Not Started",
    val avgSpeed: Double = -1.0,
    val maxSpeed: Double = -1.0,
    val speed: Double = -1.0,
    val wasEverPaused: Boolean = false,
    val wasPermissionsViolated: Boolean = false,
    val wasMockOn: Boolean = false,
    val wasMockLocationUsed: Boolean = false,
    val wasBatteryOptimizationEnabled: Boolean = false,
    val wasPowerSaverEnabled: Boolean = false,
    val wasPhoneShutDown: Boolean = false,
    val wasAppKilled: Boolean = false,
    val foregroundServiceTerminated: Boolean = false,
    val useOdometer: Boolean = false,
    val odometerOcr: Boolean = false,
    val odometerCalculationExpense: Boolean = false,
    val endTrackMilesTime: String = "",
    val autoDiscardTrackMiles: Boolean = false,
    val odometerStartUrl: String = "",
    val odometerEndUrl: String = "",
    val odometerStartReading: String = "",
    val odometerEndReading: String = "",
    val odometerStartOcr: String = "NA",
    val odometerEndOcr: String = "NA",
    val odometerStartPhotoTime: Long = -1L,
    val odometerEndPhotoTime: Long = -1L,
    val odometerNotWorking: Boolean = false,
    val trackerPausedTimeMins: Long = 0L,
    val trackerInactivityTimeMins: Long = 0L,
    val batteryOptimizationOnTimeMins: Long = 0L,
    val powerSaverOnTimeMins: Long = 0L,
    val totalBatteryOptimizationOnTime: Long = 0L,
    val totalPowerSaverOnTime: Long = 0L,
    val originalDistance: Double = 0.0,
    val mockDistance: Double = 0.0,
    val abnormalDistance: Double = 0.0,
    val spikeDistance: Double = 0.0,
    val odometerDistance: Double = 0.0,
    val cleanedDistance: Double = 0.0,
    val smartDistanceFinal: Double = 0.0,
    val startAppVersion: String = "-",
    val endAppVersion: String = "-",
    val startDeviceVersion: String = "-",
    val endDeviceVersion: String = "-",
    val lowRamDevice: Boolean = false,
    val syncIntervalTime: Long = 30_000L,
    val lastSyncedTime: Long = -1L,
    val totalLocationPoints: Long = 0L,
    val unsyncedLocationPoints: Long = 0L,
    val pauseStartTimestamp: Long = -1L,
    val lastHardwareEventText: String = "",
    val lastHardwareEventTime: Long = -1L,
    val pausedLocationId: String = "",
    val lastLocationProvider: String = "",
    val lastLocationAccuracy: Double = -1.0,
    val appKilledCount: Long = 0L,
    val phoneShutdownCount: Long = 0L,
    val foregroundServiceTerminatedCount: Long = 0L,
    val loggedOutCount: Long = 0L,
    val force: Long = 0L,
    val notes: String = "-",
    val processorFormDataJson: String? = null,
    val expenseFormDataJson: String? = null,
    val employeesJson: String? = null,
    val violationRemarks: String? = null,
    val attachmentsJson: String? = null,
    val officeId: Long? = null,
    val entityId: Long? = null,
    val tripId: String? = null,
    val tripV2Id: String? = null,
    val itineraryId: String? = null,
    val roundTrip: Boolean = false,
    val locationV2: Boolean = true,
    val journeyDate: Long? = null,
    val journeyTime: Long? = null,
    val serverUploaded: Boolean = false,
    val submissionTime: Long = 0L,
    val isDeepLinkJourney: Boolean = false,
    val submittedAmount: Double = 0.0,
    val submittedAmountCurrency: String = "INR",
    val transId: String? = null,
    @ColumnInfo(name = "has_local_data")
    val hasLocalData: Boolean = true,
    // P3.3: set to the voucher's number once this trip has been selected into a submitted
    // voucher, so it can't fund a second one (mirrors a common server-side remaining-voucher-count
    // check). Null means "unclaimed, still eligible".
    val claimedByVoucherNumber: String? = null,
    // P11.3: the head-home destination this trip was auto-classified toward, stamped at trip start
    // when destination mode was active (null = not headed anywhere in particular).
    val destinationTag: String? = null,
) {
    init {
        require(routeId.isNotBlank()) { "RouteId cannot be blank" }
        require(name.isNotBlank()) { "Track name cannot be blank" }
    }

    fun isOngoing(): Boolean = !isCompleted

    fun getStatus(): String =
        when {
            isDraft -> "Draft"
            isDiscarded -> "Discarded"
            !isCompleted -> "Ongoing"
            wasPermissionsViolated -> "Completed (with issues)"
            else -> "Completed"
        }
}
