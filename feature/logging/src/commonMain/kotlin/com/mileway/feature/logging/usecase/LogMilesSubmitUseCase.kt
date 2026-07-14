package com.mileway.feature.logging.usecase

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.network.api.MilewayNetworkApi
import com.siddharth.kmp.offlineoutbox.SubmitOutbox
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Submits a Log Miles journey durably: the request is enqueued into [outbox] (survives process
 * death) before the network call, then marked SUBMITTED/FAILED once the call resolves — the same
 * durable-draft-queue idiom every other submit flow shares. Extracted from the ViewModel (A.3c) so
 * the network call and its error wrapping live in one testable place and the VM stays a thin
 * orchestrator.
 */
class LogMilesSubmitUseCase(
    private val api: MilewayNetworkApi,
    private val outbox: SubmitOutbox<LogMilesSubmitRequestV2>,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(request: LogMilesSubmitRequestV2): Result<ExpenseSubmissionResponse> {
        val uniqueKey = Uuid.random().toString()
        outbox.enqueue(FORM_KEY, uniqueKey, request)
        return runCatching { api.logMiles(request) }
            .onSuccess { outbox.markSubmitted(FORM_KEY, uniqueKey) }
            .onFailure { e -> outbox.markFailed(FORM_KEY, uniqueKey, e.message ?: "Submission failed") }
    }

    companion object {
        const val FORM_KEY = "log_miles"
    }
}
