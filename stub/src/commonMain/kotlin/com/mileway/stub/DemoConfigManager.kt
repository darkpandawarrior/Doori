package com.mileway.stub

import com.mileway.core.data.model.network.UserConfigResponseV2
import com.mileway.core.data.model.state.LogMilesPluginConfig
import com.mileway.core.data.model.state.ProfileConfig
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.data.result.NetworkError
import com.mileway.core.data.result.NetworkResult
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.platform.UpdateConfig
import com.mileway.core.platform.UpdateMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DemoConfigManager(
    private val updateConfig: UpdateConfig =
        UpdateConfig(
            enabled = true,
            mode = UpdateMode.FLEXIBLE,
            minSupportedVersionCode = 1L,
            staleDays = 7,
            priority = 0,
        ),
    private val featureFlags: Map<String, Boolean> =
        mapOf(
            "referralEnabled" to true,
            "inAppReviewEnabled" to true,
            "inAppUpdateEnabled" to true,
            "shareEnabled" to true,
        ),
    private val killSwitch: Boolean = false,
) : ConfigProvider {
    val configState: StateFlow<NetworkResult<UserConfigResponseV2, NetworkError>> =
        MutableStateFlow(NetworkResult.Success(DemoMockData.userConfig()))

    fun getConfig(): UserConfigResponseV2 = DemoMockData.userConfig()

    private val demoProfile =
        ProfileConfig(
            code = "EMP001",
            name = "Demo User",
            email = "demo@mileway.app",
            tenant = "DEMO",
            currency = "INR",
        )

    override fun getTrackMilesConfig(): TrackMilesPluginConfig =
        TrackMilesPluginConfig(
            isTrackMilesEnabled = true, trackMilesV2 = true, draftTrackMiles = true,
            allowPauseTrackMiles = true, allowExpenseCreation = true,
            isOdometerMandatory = false, odometerOcrEnabled = false,
            geoCheckInEnabled = true, calculateDistanceOnBackend = false,
            autoDiscardTrackMileage = false, skipOdometer = true,
            showTrackingOverlay = true, saveTrackMilesEnabled = true,
            isDiscardJourneyEnabled = true, allowManualCheckIn = true,
            enableNetworkSyncing = true, minTrackingIntervalSeconds = 10L,
            tenantCode = "DEMO", currency = "INR", profile = demoProfile,
        )

    override fun getLogMilesConfig(): LogMilesPluginConfig =
        LogMilesPluginConfig(
            logMilesEnabled = true,
            isMilesEditable = true,
            draftLogMiles = true,
            multiServiceLogMiles = false,
            service = "Own Car",
            currency = "INR",
            tenantCode = "DEMO",
            profile = demoProfile,
        )

    override fun isMilesEnabled(): Boolean = true

    override fun isLogMilesEnabled(): Boolean = true

    override fun getCurrency(): String = "INR"

    fun isTrackMilesV2Enabled(): Boolean = true

    fun isDraftTrackMilesEnabled(): Boolean = true

    fun isGeoCheckInEnabled(): Boolean = true

    fun isManualCheckInEnabled(): Boolean = true

    fun isOdometerMandatory(): Boolean = false

    fun isAutoDiscardEnabled(): Boolean = false

    fun isCalculateDistanceOnBackend(): Boolean = false

    fun isMultiServiceLogMiles(): Boolean = false

    fun getMileageTimeThreshold(): Int = 10

    /** Default geofence radius in metres for geo check-in validation. */
    fun getDefaultGeoCheckInRadiusMeters(): Double = 100.0

    /** Returns the list of mock check-in locations for local geofence validation. */
    fun getMockCheckInLocations(): List<MockCheckInLocation> = DemoMockData.checkInLocations()

    /** Whether the expense form shows the office-selection picker. */
    fun isOfficeSelectionOnExpenseEnabled(): Boolean = true

    /** Whether inter-office journeys are enabled in the demo. */
    fun isInterOfficeEnabled(): Boolean = true

    /** Whether odometer photo upload is enabled for the given entry point (e.g. "start", "end"). */
    fun isOdometerUploadEnabled(source: String): Boolean = true

    /** Branch check-in gate before starting a journey. Default off; debug-flippable later. */
    override fun isBranchCheckInRequired(): Boolean = false

    /** Consent text shown by the start-journey disclaimer sheet; null hides the sheet. */
    override fun getJourneyDisclaimer(): String? =
        "This demo records your trip locally on this device only. " +
            "By starting a journey you consent to location tracking while the trip is active."

    /** Maximum reimbursable distance per day, in kilometres. */
    override fun getMaxDailyDistanceKm(): Double = 10.0

    override fun getOffices() = PolicyMockData.offices()

    override fun getBusinessEntities() = PolicyMockData.businessEntities()

    override fun isOfficeSelectionRequired(): Boolean = false

    /** Returns the 4 check-in type labels available in the demo. */
    override fun getCheckInTypes(): List<String> = listOf("Office Check-In", "Client Visit", "Site Inspection", "Meeting Point")

    /** Returns a list of (fieldLabel, fieldHint) pairs for the given check-in type. */
    override fun getCheckInFormSchema(type: String): List<Pair<String, String>> =
        when (type) {
            "Office Check-In" -> listOf("Desk number" to "E.g. D-42", "Floor" to "1–10")
            "Client Visit" -> listOf("Contact name" to "Name of the client contact", "Meeting purpose" to "Purpose of visit")
            "Site Inspection" -> listOf("Site ID" to "E.g. SITE-001", "Safety clearance" to "Yes / No")
            "Meeting Point" -> listOf("Reference number" to "E.g. MTG-2024", "Host name" to "Name of the host")
            else -> emptyList()
        }

    override fun getDemoLat(): Double = 12.927923

    override fun getDemoLng(): Double = 77.627108

    override fun getDemoAccuracyLabel(): String = "± 8 m (GPS)"

    override fun getVendorCenters() = VendorMockData.vendorCenters()

    override fun getGeoCheckInRadiusMeters(): Double = 100.0

    // ─── V15 gating surface (demo-sane defaults; constructor-overridable for tests / env wiring) ───

    override fun getUpdateConfig(): UpdateConfig = updateConfig

    override fun getFeatureFlags(): Map<String, Boolean> = featureFlags

    override fun isKillSwitchOn(): Boolean = killSwitch
}
