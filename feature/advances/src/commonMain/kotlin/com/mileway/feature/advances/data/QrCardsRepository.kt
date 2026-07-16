package com.mileway.feature.advances.data

import com.mileway.feature.advances.model.AdvanceRequest
import com.mileway.feature.advances.model.AdvanceRequestStatus
import com.mileway.feature.advances.model.AdvanceSection
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.model.SubmittedRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** QR-card-wallet data source. PLAN_V35.P3 — offline mock impl only, no network. */
interface QrCardsRepository {
    fun activeQrCards(): Flow<List<QrCard>>

    fun pastQrCards(): Flow<List<QrCard>>

    /** First = whether the request form must show a type selector; second = the QR type list. */
    fun qrTypes(): Pair<Boolean, List<AdvanceType>>

    suspend fun submitQrRequest(
        type: String?,
        amount: Double,
        title: String,
        description: String,
        cardId: String?,
        declarationAccepted: Boolean,
    ): Result<SubmittedRequest>

    fun topUpHistory(cardId: String): Flow<List<AdvanceTransaction>>
}

internal class MockQrCardsRepository(
    private val store: AdvancesRequestStore,
) : QrCardsRepository {
    override fun activeQrCards(): Flow<List<QrCard>> = flowOf(AdvancesMockData.activeQrCards)

    override fun pastQrCards(): Flow<List<QrCard>> = flowOf(AdvancesMockData.pastQrCards)

    override fun qrTypes(): Pair<Boolean, List<AdvanceType>> = true to AdvancesMockData.qrTypes

    override suspend fun submitQrRequest(
        type: String?,
        amount: Double,
        title: String,
        description: String,
        cardId: String?,
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
                    type = type ?: AdvancesMockData.qrTypes.first().title,
                    section = AdvanceSection.QR,
                    status = AdvanceRequestStatus.APPROVAL,
                    createdAtMs = AdvancesMockData.BASE_MS,
                )
            }
        return Result.success(SubmittedRequest(permissionId))
    }

    override fun topUpHistory(cardId: String): Flow<List<AdvanceTransaction>> = flowOf(AdvancesMockData.qrTopUps(cardId.toLongOrNull() ?: -1L))
}
