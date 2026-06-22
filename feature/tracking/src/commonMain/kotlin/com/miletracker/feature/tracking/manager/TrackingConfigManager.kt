package com.miletracker.feature.tracking.manager

import com.miletracker.core.data.model.state.LogMilesPluginConfig
import com.miletracker.core.data.model.state.TrackMilesPluginConfig
import com.miletracker.core.network.config.ConfigProvider
import com.miletracker.core.network.model.VendorCenter

class TrackingConfigManager(private val configProvider: ConfigProvider) {
    fun getTrackMilesConfig(): TrackMilesPluginConfig = configProvider.getTrackMilesConfig()

    fun getLogMilesConfig(): LogMilesPluginConfig = configProvider.getLogMilesConfig()

    fun isMilesEnabled(): Boolean = configProvider.isMilesEnabled()

    fun isLogMilesEnabled(): Boolean = configProvider.isLogMilesEnabled()

    fun getCurrency(): String = configProvider.getCurrency()

    fun getJourneyDisclaimer(): String? = configProvider.getJourneyDisclaimer()

    fun getMaxDailyDistanceKm(): Double = configProvider.getMaxDailyDistanceKm()

    fun isBranchCheckInRequired(): Boolean = configProvider.isBranchCheckInRequired()

    fun getOffices() = configProvider.getOffices()

    fun getBusinessEntities() = configProvider.getBusinessEntities()

    fun isOfficeSelectionRequired(): Boolean = configProvider.isOfficeSelectionRequired()

    fun getCheckInTypes(): List<String> = configProvider.getCheckInTypes()

    fun getCheckInFormSchema(type: String): List<Pair<String, String>> = configProvider.getCheckInFormSchema(type)

    fun getDemoLat(): Double = configProvider.getDemoLat()

    fun getDemoLng(): Double = configProvider.getDemoLng()

    fun getDemoAccuracyLabel(): String = configProvider.getDemoAccuracyLabel()

    fun getVendorCenters(): List<VendorCenter> = configProvider.getVendorCenters()

    fun getGeoCheckInRadiusMeters(): Double = configProvider.getGeoCheckInRadiusMeters()

    fun getMaxAccuracyThresholdM(): Double = configProvider.getMaxAccuracyThresholdM()
}
