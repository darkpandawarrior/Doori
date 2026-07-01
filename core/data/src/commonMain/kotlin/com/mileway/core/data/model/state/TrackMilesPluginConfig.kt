package com.mileway.core.data.model.state

data class TrackMilesPluginConfig(
    val isTrackMilesEnabled: Boolean = true,
    val trackMilesV2: Boolean = true,
    val draftTrackMiles: Boolean = true,
    val allowPauseTrackMiles: Boolean = true,
    val allowExpenseCreation: Boolean = true,
    val isOdometerMandatory: Boolean = false,
    val odometerOcrEnabled: Boolean = false,
    val geoCheckInEnabled: Boolean = false,
    val calculateDistanceOnBackend: Boolean = false,
    val autoDiscardTrackMileage: Boolean = false,
    val skipOdometer: Boolean = true,
    val skipBatteryOptimization: Boolean = true,
    val skipPowerSaver: Boolean = true,
    val showTrackingOverlay: Boolean = true,
    val saveTrackMilesEnabled: Boolean = true,
    val isDiscardJourneyEnabled: Boolean = true,
    val allowManualCheckIn: Boolean = true,
    val enableNetworkSyncing: Boolean = true,
    val minTrackingIntervalSeconds: Long = 10L,
    val trackMilesLocationAccuracyRadius: Double = 250.0,
    val syncTimeIntervalMinsThresholdForTrackMileage: Long = 10L,
    val tenantCode: String = "DEMO",
    val endTrackMilesTime: String = "-",
    val trackMilesService: String = "Own Car",
    val logMilesEnabled: Boolean = true,
    val currency: String = "INR",
    val profile: ProfileConfig? = null,
)

data class LogMilesPluginConfig(
    val logMilesEnabled: Boolean = true,
    val isMilesEditable: Boolean = true,
    val draftLogMiles: Boolean = true,
    val multiServiceLogMiles: Boolean = false,
    val disableLocationInLogMiles: Boolean = false,
    val service: String = "Own Car",
    val currency: String = "INR",
    val tenantCode: String = "DEMO",
    val profile: ProfileConfig? = null,
)

data class ProfileConfig(
    val code: String = "EMP001",
    val name: String = "Demo User",
    val email: String = "demo@mileway.app",
    val tenant: String = "DEMO",
    val currency: String = "INR",
)
