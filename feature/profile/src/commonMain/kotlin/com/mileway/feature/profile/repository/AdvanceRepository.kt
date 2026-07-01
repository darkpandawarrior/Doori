package com.mileway.feature.profile.repository

import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.CardStatus
import com.mileway.feature.profile.model.CardTransaction
import com.mileway.feature.profile.model.CardType
import com.mileway.feature.profile.model.CorporateCard

class AdvanceRepository {
    private val baseMs = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    val advanceRecords =
        listOf(
            AdvanceRecord(
                id = "ADV-001",
                amountRupees = 8000.0,
                purpose = "Field visit expenses – Nashik",
                status = AdvanceStatus.DISBURSED,
                requestedDateMs = baseMs - 30 * dayMs,
                requiredByDate = "2024-01-05",
            ),
            AdvanceRecord(
                id = "ADV-002",
                amountRupees = 15000.0,
                purpose = "Outstation client onboarding trip",
                status = AdvanceStatus.APPROVED,
                requestedDateMs = baseMs - 10 * dayMs,
                requiredByDate = "2024-01-25",
            ),
            AdvanceRecord(
                id = "ADV-003",
                amountRupees = 5500.0,
                purpose = "Training workshop materials",
                status = AdvanceStatus.UNDER_REVIEW,
                requestedDateMs = baseMs - 3 * dayMs,
                requiredByDate = "2024-02-01",
            ),
            AdvanceRecord(
                id = "ADV-004",
                amountRupees = 25000.0,
                purpose = "Annual conference sponsorship deposit",
                status = AdvanceStatus.PENDING,
                requestedDateMs = baseMs - 1 * dayMs,
                requiredByDate = "2024-02-10",
            ),
        )

    val cards =
        listOf(
            CorporateCard(
                id = "CARD-001",
                lastFourDigits = "4821",
                cardType = CardType.VISA,
                holderName = "Demo User",
                balanceRupees = 42_380.0,
                status = CardStatus.ACTIVE,
                expiryDate = "08/27",
                creditLimitRupees = 100_000.0,
            ),
            CorporateCard(
                id = "CARD-002",
                lastFourDigits = "9934",
                cardType = CardType.MASTERCARD,
                holderName = "Demo User",
                balanceRupees = 0.0,
                status = CardStatus.BLOCKED,
                expiryDate = "03/26",
                creditLimitRupees = 50_000.0,
            ),
        )

    val cardTransactions =
        listOf(
            CardTransaction("TXN-001", "CARD-001", "Uber Eats", 840.0, baseMs - 1 * dayMs, "Food"),
            CardTransaction("TXN-002", "CARD-001", "IndiGo Airlines", 8200.0, baseMs - 3 * dayMs, "Travel"),
            CardTransaction("TXN-003", "CARD-001", "Marriott Pune", 9500.0, baseMs - 5 * dayMs, "Accommodation"),
            CardTransaction("TXN-004", "CARD-001", "Amazon Business", 2100.0, baseMs - 8 * dayMs, "Office Supplies"),
            CardTransaction("TXN-005", "CARD-001", "Swiggy – Team Lunch", 1650.0, baseMs - 12 * dayMs, "Food"),
            CardTransaction("TXN-006", "CARD-001", "Ola Cabs", 480.0, baseMs - 15 * dayMs, "Travel"),
        )

    fun getCardById(id: String) = cards.find { it.id == id }

    fun getTransactionsForCard(cardId: String) = cardTransactions.filter { it.cardId == cardId }
}
