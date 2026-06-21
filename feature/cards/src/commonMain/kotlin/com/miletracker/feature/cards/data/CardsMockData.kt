package com.miletracker.feature.cards.data

import com.miletracker.feature.cards.model.ApprovalStepModel
import com.miletracker.feature.cards.model.ApprovalStepStatus
import com.miletracker.feature.cards.model.CardModel
import com.miletracker.feature.cards.model.CardRequestModel
import com.miletracker.feature.cards.model.CardRequestStatus
import com.miletracker.feature.cards.model.CardStatus
import com.miletracker.feature.cards.model.CardTransactionModel
import com.miletracker.feature.cards.model.CardTxnClaimStatus
import com.miletracker.feature.cards.model.CardTypeModel
import kotlin.time.Clock

/**
 * Q.2 — offline mock data for the cards feature (no network).
 * One [CardsMockData] builder is parameterized by a localized [CardsStrings] bundle, so EN/AR/HI
 * share the same deterministic structure. The [CardsMockDataProviderFactory] picks the bundle by locale.
 */
interface CardsMockDataProvider {
    fun virtualCards(): List<CardModel>

    fun physicalCards(): List<CardModel>

    fun cardById(id: Long): CardModel?

    fun cardTypes(): List<CardTypeModel>

    fun transactions(cardId: Long): List<CardTransactionModel>

    fun requests(): List<CardRequestModel>
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

internal class CardsMockData(private val s: CardsStrings) : CardsMockDataProvider {
    private val now = Clock.System.now().toEpochMilliseconds()

    private fun daysAgo(n: Int): Long = now - n * DAY_MILLIS

    private val cards =
        listOf(
            CardModel(
                id = 1L,
                cardNumber = "4291",
                cardHolderName = s.holderName,
                cardType = s.typeTravel,
                cardTypeId = 1L,
                issuer = s.issuer,
                validThru = "08/27",
                scheme = "VISA",
                balance = 4200.0,
                limit = 10000.0,
                status = CardStatus.ACTIVE,
                createdAt = daysAgo(120),
                lastUpdatedAt = daysAgo(2),
                employeeEmail = s.email,
                monthlyLimit = 10000.0,
            ),
            CardModel(
                id = 2L,
                cardNumber = "8830",
                cardHolderName = s.holderName,
                cardType = s.typeProcurement,
                cardTypeId = 3L,
                issuer = s.issuer,
                validThru = "11/26",
                scheme = "MASTERCARD",
                balance = 1875.5,
                limit = 5000.0,
                status = CardStatus.ACTIVE,
                createdAt = daysAgo(90),
                lastUpdatedAt = daysAgo(1),
                employeeEmail = s.email,
                monthlyLimit = 5000.0,
            ),
            CardModel(
                id = 3L,
                cardNumber = "5102",
                cardHolderName = s.holderName,
                cardType = s.typeCorporate,
                cardTypeId = 4L,
                issuer = s.issuer,
                validThru = "03/28",
                scheme = "VISA",
                balance = 620.0,
                limit = 3000.0,
                status = CardStatus.FROZEN,
                createdAt = daysAgo(60),
                lastUpdatedAt = daysAgo(5),
                isFrozen = true,
                employeeEmail = s.email,
                monthlyLimit = 3000.0,
            ),
            CardModel(
                id = 4L,
                cardNumber = "7744",
                cardHolderName = s.holderName,
                cardType = s.typeFuel,
                cardTypeId = 2L,
                issuer = s.issuer,
                validThru = "05/27",
                scheme = "VISA",
                balance = 0.0,
                limit = 2000.0,
                status = CardStatus.KYC_PENDING,
                createdAt = daysAgo(7),
                lastUpdatedAt = daysAgo(7),
                isKycPending = true,
                employeeEmail = s.email,
                monthlyLimit = 2000.0,
            ),
        )

    override fun virtualCards(): List<CardModel> = cards

    override fun physicalCards(): List<CardModel> = emptyList()

    override fun cardById(id: Long): CardModel? = cards.firstOrNull { it.id == id }

    override fun cardTypes(): List<CardTypeModel> =
        listOf(
            CardTypeModel(1L, s.typeTravel, s.typeTravelDesc, "VISA", isDefault = true, defaultMonthlyLimit = 10000.0),
            CardTypeModel(2L, s.typeFuel, s.typeFuelDesc, "VISA", defaultMonthlyLimit = 2000.0),
            CardTypeModel(3L, s.typeProcurement, s.typeProcurementDesc, "MASTERCARD", defaultMonthlyLimit = 5000.0),
            CardTypeModel(4L, s.typeCorporate, s.typeCorporateDesc, "VISA", isAiSuggested = true, defaultMonthlyLimit = 3000.0),
            CardTypeModel(5L, s.typeIt, s.typeItDesc, "MASTERCARD", isAiSuggested = true, defaultMonthlyLimit = 1500.0),
        )

    override fun transactions(cardId: Long): List<CardTransactionModel> {
        val statuses = CardTxnClaimStatus.entries
        return (0 until 6).map { i ->
            CardTransactionModel(
                id = cardId * 100L + i,
                cardId = cardId,
                merchantName = s.merchants[i % s.merchants.size],
                amount = 50.0 + i * 37 + cardId * 10,
                date = daysAgo(i * 3 + 1),
                category = s.categories[i % s.categories.size],
                claimStatus = statuses[i % statuses.size],
                txnNumber = "TXN-$cardId${100 + i}",
            )
        }
    }

    override fun requests(): List<CardRequestModel> =
        listOf(
            CardRequestModel(
                id = 201L,
                cardTypeId = 5L,
                status = CardRequestStatus.IN_PROGRESS,
                requestDate = daysAgo(3),
                cardType = s.typeIt,
                requesterName = s.holderName,
                requesterGrade = "M3",
                reason = s.reasonIt,
                requestedLimit = 1500.0,
                approvalSteps =
                    listOf(
                        ApprovalStepModel(1L, s.stepManager, ApprovalStepStatus.APPROVED, approverName = s.approverManager, order = 0),
                        ApprovalStepModel(2L, s.stepFinance, ApprovalStepStatus.PENDING, approverName = s.approverFinance, order = 1),
                    ),
            ),
            CardRequestModel(
                id = 202L,
                cardTypeId = 1L,
                status = CardRequestStatus.APPROVED,
                requestDate = daysAgo(20),
                cardType = s.typeTravel,
                requesterName = s.holderName,
                requesterGrade = "M3",
                reason = s.reasonTravel,
                requestedLimit = 10000.0,
                approvalSteps =
                    listOf(
                        ApprovalStepModel(1L, s.stepManager, ApprovalStepStatus.APPROVED, approverName = s.approverManager, order = 0),
                        ApprovalStepModel(2L, s.stepFinance, ApprovalStepStatus.APPROVED, approverName = s.approverFinance, order = 1),
                    ),
            ),
        )
}

/** EN / AR / HI mock provider selector. */
object CardsMockDataProviderFactory {
    fun provider(localeTag: String = currentLocaleTag()): CardsMockDataProvider =
        when (localeTag.lowercase().take(2)) {
            "ar" -> CardsMockData(ArabicCardsStrings)
            "hi" -> CardsMockData(HindiCardsStrings)
            else -> CardsMockData(EnglishCardsStrings)
        }
}
