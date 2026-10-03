package com.mileway.feature.tracking.detection

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.siddharth.kmp.appshell.AppPermission
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/** An optional departure fence selected by the caller; no continuous GPS is needed to wait. */
@Serializable
data class DriveDeparture(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(radiusMeters.isFinite() && radiusMeters >= 100f)
    }
}

/** Only the trip setup is persisted while waiting, never an active Room session. */
@Serializable
data class PendingDrive(
    val routeId: String,
    val name: String,
    val vehicleType: String,
    val vehiclePricing: Double,
    val employeeCode: String,
    val accountEmail: String,
    val tenant: String,
    val destinationTag: String? = null,
) {
    fun recordingAt(now: Long): SavedTrack =
        SavedTrack(
            routeId = routeId,
            name = name,
            selectedVehicleType = vehicleType,
            vehiclePricing = vehiclePricing,
            startedByEmployeeCode = employeeCode,
            startedByAccountEmail = accountEmail,
            startedByTenant = tenant,
            destinationTag = destinationTag,
            startLatitude = 0.0,
            startLongitude = 0.0,
            endLatitude = 0.0,
            endLongitude = 0.0,
            pausedLatitude = 0.0,
            pausedLongitude = 0.0,
            startTime = now,
            startedAtTimestamp = now,
            createdAt = now,
            endTime = -1L,
            duration = 0L,
            distance = 0.0,
        )
}

/** Platform source for idle drive-wake. Implementations must not start recording in [arm]. */
interface DriveStartSource {
    val waiting: StateFlow<Boolean>
    val waitLabel: String get() = "Wait for driving"
    val requiredPermissions: List<AppPermission> get() = listOf(AppPermission.LOCATION)

    /** Returns false if the platform cannot safely arm; permission prompts belong to the foreground UI. */
    suspend fun arm(
        drive: PendingDrive,
        departure: DriveDeparture? = null,
    ): Boolean

    suspend fun disarm()
}

/** Creates the trip at motion time, then starts the active recording controller. */
class DetectedDriveRecorder(
    private val tracks: SavedTrackRepository,
    private val controller: TrackingController,
) {
    // Platform start failures include Android security/service exceptions unavailable in commonMain.
    @Suppress("TooGenericExceptionCaught")
    suspend fun start(drive: PendingDrive) {
        if (tracks.getActiveTrack()?.routeId?.let { it != drive.routeId } == true) return
        val now = Clock.System.now().toEpochMilliseconds()
        val inserted = tracks.getByRouteId(drive.routeId) == null
        if (inserted) tracks.insert(drive.recordingAt(now))
        try {
            controller.start(drive.routeId)
        } catch (exception: Exception) {
            if (inserted) tracks.delete(drive.routeId)
            throw exception
        }
    }
}

/** Always-upgrade requests are foreground-only; background SLC requires an existing Always grant. */
object IosDriveWakePolicy {
    fun mayRequestAlways(
        isForeground: Boolean,
        hasWhenInUse: Boolean,
    ): Boolean = isForeground && hasWhenInUse

    fun mayMonitorSignificantChanges(hasAlways: Boolean): Boolean = hasAlways
}

/** SLC's initial/cached fixes and walking-speed changes must not escalate to continuous GPS. */
object SignificantDriveFixPolicy {
    fun mayWake(
        hasAlways: Boolean,
        ageSeconds: Double,
        accuracyMeters: Double,
        speedMetersPerSecond: Double,
    ): Boolean =
        hasAlways &&
            ageSeconds in 0.0..30.0 &&
            accuracyMeters in 0.0..100.0 &&
            speedMetersPerSecond.isFinite() &&
            speedMetersPerSecond >= 5.0
}
