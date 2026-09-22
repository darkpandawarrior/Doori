package com.mileway.feature.profile.repository

import com.mileway.core.data.util.MillisPerDay
import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.AdvanceType
import com.mileway.feature.profile.model.ApproverStep
import com.mileway.feature.profile.model.ApproverStepStatus
import com.mileway.feature.profile.model.CardStatus
import com.mileway.feature.profile.model.CardTransaction
import com.mileway.feature.profile.model.CardType
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.profile.model.TimelineEntry

class AdvanceRepository {
    private val baseMs = 1_700_000_000_000L

    val advanceRecords =
        listOf(
            AdvanceRecord(
                id = "ADV-001",
                amountRupees = 8000.0,
                purpose = "Field visit expenses – Nashik",
                type = AdvanceType.FIELD_VISIT,
                status = AdvanceStatus.DISBURSED,
                requestedDateMs = baseMs - 30 * MillisPerDay,
                requiredByDate = "2024-01-05",
                approverChain =
                    listOf(
                        ApproverStep("Rohan Mehta", "Manager Approval", ApproverStepStatus.APPROVED),
                        ApproverStep("Finance Desk", "Disbursement", ApproverStepStatus.APPROVED),
                    ),
                timeline =
                    listOf(
                        TimelineEntry("Requested", baseMs - 30 * MillisPerDay),
                        TimelineEntry("Approved", baseMs - 28 * MillisPerDay),
                        TimelineEntry("Disbursed", baseMs - 27 * MillisPerDay),
                    ),
            ),
            AdvanceRecord(
                id = "ADV-002",
                amountRupees = 15000.0,
                purpose = "Outstation client onboarding trip",
                type = AdvanceType.CLIENT_ONBOARDING,
                status = AdvanceStatus.APPROVED,
                requestedDateMs = baseMs - 10 * MillisPerDay,
                requiredByDate = "2024-01-25",
                approverChain =
                    listOf(
                        ApproverStep("Rohan Mehta", "Manager Approval", ApproverStepStatus.APPROVED),
                        ApproverStep("Finance Desk", "Disbursement", ApproverStepStatus.PENDING),
                    ),
                timeline =
                    listOf(
                        TimelineEntry("Requested", baseMs - 10 * MillisPerDay),
                        TimelineEntry("Approved", baseMs - 8 * MillisPerDay),
                    ),
            ),
            AdvanceRecord(
                id = "ADV-003",
                amountRupees = 5500.0,
                purpose = "Training workshop materials",
                type = AdvanceType.TRAINING,
                status = AdvanceStatus.UNDER_REVIEW,
                requestedDateMs = baseMs - 3 * MillisPerDay,
                requiredByDate = "2024-02-01",
                approverChain =
                    listOf(
                        ApproverStep("Rohan Mehta", "Manager Approval", ApproverStepStatus.PENDING),
                    ),
                timeline =
                    listOf(
                        TimelineEntry("Requested", baseMs - 3 * MillisPerDay),
                    ),
            ),
            AdvanceRecord(
                id = "ADV-004",
                amountRupees = 25000.0,
                purpose = "Annual conference sponsorship deposit",
                status = AdvanceStatus.PENDING,
                requestedDateMs = baseMs - 1 * MillisPerDay,
                requiredByDate = "2024-02-10",
                timeline =
                    listOf(
                        TimelineEntry("Requested", baseMs - 1 * MillisPerDay),
                    ),
            ),
            AdvanceRecord(
                id = "ADV-005",
                amountRupees = 12000.0,
                purpose = "Vendor site visit – Aurangabad",
                type = AdvanceType.FIELD_VISIT,
                status = AdvanceStatus.REJECTED,
                requestedDateMs = baseMs - 20 * MillisPerDay,
                requiredByDate = "2023-12-20",
                approverChain =
                    listOf(
                        ApproverStep("Rohan Mehta", "Manager Approval", ApproverStepStatus.REJECTED),
                        ApproverStep("Finance Desk", "Disbursement", ApproverStepStatus.PENDING),
                    ),
                timeline =
                    listOf(
                        TimelineEntry("Requested", baseMs - 20 * MillisPerDay),
                        TimelineEntry("Rejected", baseMs - 18 * MillisPerDay),
                    ),
                declineReason = "Duplicate request – an active advance already covers this trip.",
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
            CardTransaction("TXN-001", "CARD-001", "Uber Eats", 840.0, baseMs - 1 * MillisPerDay, "Food"),
            CardTransaction("TXN-002", "CARD-001", "IndiGo Airlines", 8200.0, baseMs - 3 * MillisPerDay, "Travel"),
            CardTransaction("TXN-003", "CARD-001", "Marriott Pune", 9500.0, baseMs - 5 * MillisPerDay, "Accommodation"),
            CardTransaction("TXN-004", "CARD-001", "Amazon Business", 2100.0, baseMs - 8 * MillisPerDay, "Office Supplies"),
            CardTransaction("TXN-005", "CARD-001", "Swiggy – Team Lunch", 1650.0, baseMs - 12 * MillisPerDay, "Food"),
            CardTransaction("TXN-006", "CARD-001", "Ola Cabs", 480.0, baseMs - 15 * MillisPerDay, "Travel"),
        )

    fun getCardById(id: String) = cards.find { it.id == id }

    fun getTransactionsForCard(cardId: String) = cardTransactions.filter { it.cardId == cardId }
}
