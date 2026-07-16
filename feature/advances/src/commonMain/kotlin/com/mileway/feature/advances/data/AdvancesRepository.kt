package com.mileway.feature.advances.data

import com.mileway.feature.advances.model.AdvanceRequest
import com.mileway.feature.advances.model.AdvanceRequestStatus
import com.mileway.feature.advances.model.AdvanceSection
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.SubmittedRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Petty-advance-wallet data source. PLAN_V35.P3 — offline mock impl only, no network. */
interface AdvancesRepository {
    fun activePettyCards(): Flow<List<PettyCard>>

    fun pastPettyCards(): Flow<List<PettyCard>>

    fun pettyTransactions(cardId: String): Flow<List<AdvanceTransaction>>

    fun pettyTypes(): List<AdvanceType>

    suspend fun submitPettyRequest(
        type: String?,
        amount: Double,
        title: String,
        description: String,
        startMs: Long?,
        endMs: Long?,
        declarationAccepted: Boolean,
    ): Result<SubmittedRequest>

    suspend fun rechargeCard(
        cardId: String,
        amount: Double,
        remarks: String,
    ): Result<Unit>

    fun openRequests(): Flow<List<AdvanceRequest>>

    fun closedRequests(): Flow<List<AdvanceRequest>>
}

internal class MockAdvancesRepository(
    private val store: AdvancesRequestStore,
) : AdvancesRepository {
    override fun activePettyCards(): Flow<List<PettyCard>> = flowOf(AdvancesMockData.activePettyCards)

    override fun pastPettyCards(): Flow<List<PettyCard>> = flowOf(AdvancesMockData.pastPettyCards)

    override fun pettyTransactions(cardId: String): Flow<List<AdvanceTransaction>> = flowOf(AdvancesMockData.pettyTransactions(cardId.toLongOrNull() ?: -1L))

    override fun pettyTypes(): List<AdvanceType> = AdvancesMockData.pettyTypes

    override suspend fun submitPettyRequest(
        type: String?,
        amount: Double,
        title: String,
        description: String,
        startMs: Long?,
        endMs: Long?,
        declarationAccepted: Boolean,
    ): Result<SubmittedRequest> {
        if (!declarationAccepted) {
            return Result.failure(IllegalArgumentException("Declaration must be accepted before submitting a request."))
        }
        val permissionId =
            store.submit { id ->
                AdvanceRequest(
                    id = id,
                    title = title,
                    description = description,
                    amount = amount,
                    type = type ?: AdvancesMockData.pettyTypes.first().title,
                    section = AdvanceSection.PETTY,
                    status = AdvanceRequestStatus.PENDING,
                    createdAtMs = AdvancesMockData.BASE_MS,
                )
            }
        return Result.success(SubmittedRequest(permissionId))
    }

    override suspend fun rechargeCard(
        cardId: String,
        amount: Double,
        remarks: String,
    ): Result<Unit> {
        // ponytail: mock-only — no balance mutation persisted (no local card store to write back
        // to yet); UI treats success as the refresh signal. Add persistence when :stub gains one.
        return if (amount > 0.0) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Recharge amount must be positive."))
        }
    }

    // Store holds both PETTY and QR requests (AdvanceRequest.section distinguishes them) so a
    // combined "My Requests" screen can render one timeline — same reason QrCardsRepository has
    // no separate openRequests() of its own.
    override fun openRequests(): Flow<List<AdvanceRequest>> = store.open

    override fun closedRequests(): Flow<List<AdvanceRequest>> = flowOf(AdvancesMockData.closedRequests)
}
