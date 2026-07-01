package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.network.api.MilewayNetworkApi

class VehiclePricingRepository(private val api: MilewayNetworkApi) {
    suspend fun getVehicles(trackMiles: Boolean = true): List<ApprovedVehicle> = api.vehicles(trackMiles).vehicles

    suspend fun getPricing(): Map<String, Double> = api.pricing().data
}
