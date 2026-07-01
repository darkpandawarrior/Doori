package com.mileway.feature.logging.usecase

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.network.api.MilewayNetworkApi

/**
 * Submits a Log Miles journey. Extracted from the ViewModel (A.3c) so the network call and its
 * error wrapping live in one testable place and the VM stays a thin orchestrator.
 */
class LogMilesSubmitUseCase(
    private val api: MilewayNetworkApi,
) {
    suspend operator fun invoke(request: LogMilesSubmitRequestV2): Result<ExpenseSubmissionResponse> = runCatching { api.logMiles(request) }
}
