package com.mileway.core.network.config

import com.mileway.core.data.model.state.LogMilesPluginConfig
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.network.model.BusinessEntity
import com.mileway.core.network.model.Office
import com.mileway.core.network.model.VendorCenter
import com.mileway.core.platform.UpdateConfig

interface ConfigProvider {
    fun getTrackMilesConfig(): TrackMilesPluginConfig

    fun getLogMilesConfig(): LogMilesPluginConfig

    fun isMilesEnabled(): Boolean

    fun isLogMilesEnabled(): Boolean

    fun getCurrency(): String

    /** Workspace disclaimer shown on the start-journey consent step; null = no consent. */
    fun getJourneyDisclaimer(): String? = null

    /** Policy ceiling on reimbursable distance per day, in km. */
    fun getMaxDailyDistanceKm(): Double = 10.0

    /** When true, stopping a journey requires at least one branch check-in. */
    fun isBranchCheckInRequired(): Boolean = false

    /** Offices a mileage expense can be billed against (submission picker). */
    fun getOffices(): List<Office> = emptyList()

    /** Business entities (legal companies) a submission can be filed under. */
    fun getBusinessEntities(): List<BusinessEntity> = emptyList()

    /** When true, the submission form must collect an office + entity. */
    fun isOfficeSelectionRequired(): Boolean = false

    /** Check-in type labels available in the demo. */
    fun getCheckInTypes(): List<String> = listOf("Office Check-In", "Client Visit", "Site Inspection", "Meeting Point")

    /** (label, hint) field pairs for a given check-in type. */
    fun getCheckInFormSchema(type: String): List<Pair<String, String>> = emptyList()

    /** Demo/GPS latitude (for check-in screens that need a reference position). */
    fun getDemoLat(): Double = 12.927923

    /** Demo/GPS longitude. */
    fun getDemoLng(): Double = 77.627108

    /** Human-readable accuracy label (e.g. "± 8 m (GPS)"). */
    fun getDemoAccuracyLabel(): String = "± 8 m (GPS)"

    /** Vendor / partner centers used for geo check-in proximity calculations. */
    fun getVendorCenters(): List<VendorCenter> = emptyList()

    /** Default geofence radius in metres for geo check-in validation. */
    fun getGeoCheckInRadiusMeters(): Double = 100.0

    /** Accuracy threshold (m) above which a GPS fix is persisted but not counted toward distance. */
    fun getMaxAccuracyThresholdM(): Double = 50.0

    // ─── V15 gating surface (mirrors a remote splash/config API; env/BuildConfig-overridable) ───

    /** In-app update gate config. Default = disabled (force/flexible toggle lives in [UpdateConfig.mode]). */
    fun getUpdateConfig(): UpdateConfig = UpdateConfig()

    /** Named feature flags gating optional UI (e.g. `referralEnabled`, `inAppReviewEnabled`). */
    fun getFeatureFlags(): Map<String, Boolean> = emptyMap()

    /** When true, the app shows a blocking "under maintenance" wall before any feature UI. */
    fun isKillSwitchOn(): Boolean = false
}
