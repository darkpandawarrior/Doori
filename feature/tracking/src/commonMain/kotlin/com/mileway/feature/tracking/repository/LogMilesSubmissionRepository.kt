package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.network.api.MilewayNetworkApi

class LogMilesSubmissionRepository(private val api: MilewayNetworkApi) {
    suspend fun submitLogMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse = api.logMiles(request)
}
