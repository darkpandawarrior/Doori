package com.mileway.feature.advances.data

import com.mileway.feature.advances.model.AdvanceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory open-requests list shared by [AdvancesRepository] and [QrCardsRepository] mock
 * submits — `AdvanceRequest.section` (PETTY/QR) is what lets one list back both products, mirroring
 * the reference app's combined AdvanceRequestsScreen. Constructor-injectable (not a singleton
 * object) so each Koin container — and each test — gets its own isolated mutable state.
 */
internal class AdvancesRequestStore(
    seedOpen: List<AdvanceRequest> = AdvancesMockData.openRequests,
    private var nextId: Long = AdvancesMockData.NEXT_REQUEST_ID_SEED,
) {
    val open = MutableStateFlow(seedOpen)

    /** Appends the request built from a freshly-issued id and returns that id (always > 0). */
    fun submit(buildRequest: (id: Long) -> AdvanceRequest): Long {
        val id = nextId++
        open.update { it + buildRequest(id) }
        return id
    }
}
