package com.mileway.feature.logging.repository

import com.mileway.core.data.model.network.LogMilesService
import com.mileway.core.network.api.MilewayNetworkApi

class LogMilesServiceRepository(private val api: MilewayNetworkApi) {
    suspend fun getServices(isInsideTrip: Boolean = false): List<LogMilesService> =
        api.fetchLogMilesServices(isInsideTrip).services
            ?.mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                val name = dto.name ?: return@mapNotNull null
                LogMilesService(id, name, dto.glCode ?: "")
            } ?: emptyList()
}
