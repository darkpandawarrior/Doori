package com.mileway.feature.advances.data

import com.mileway.feature.advances.model.AdvanceRequest
import com.mileway.feature.advances.model.AdvanceRequestStatus
import com.mileway.feature.advances.model.AdvanceSection
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.model.Granter
import com.mileway.feature.advances.model.GranterStatus
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.model.ReconBucket

/**
 * PLAN_V35.P3: deterministic offline seed data for the advance-wallet feature (CardsMockData
 * idiom — see feature/cards/data/CardsMockData.kt). Fixed epoch anchor, never
 * System.currentTimeMillis, so goldens/tests never drift with the clock.
 */
internal object AdvancesMockData {
    const val BASE_MS = 1_752_000_000_000L
    const val NEXT_REQUEST_ID_SEED = 1001L
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    private fun daysAgo(n: Int): Long = BASE_MS - n * DAY_MILLIS

    private fun daysFromNow(n: Int): Long = BASE_MS + n * DAY_MILLIS

    val pettyTypes =
        listOf(
            AdvanceType(1L, "Travel Petty Cash", "blue"),
            AdvanceType(2L, "Client Entertainment", "green"),
            AdvanceType(3L, "Office Supplies", "amber"),
        )

    val qrTypes =
        listOf(
            AdvanceType(101L, "Fuel QR", "orange"),
            AdvanceType(102L, "Site Visit QR", "purple"),
        )

    // Ratios: healthy 3200/5000=64% (ACTIVE), low 1500/5000=30% (LOW_BALANCE), critical 400/5000=8% (CRITICAL).
    val activePettyCards =
        listOf(
            PettyCard(
                id = 1L,
                kitNo = "PC-1001",
                amount = 5000.0,
                balance = 3200.0,
                createdAtMs = daysAgo(40),
                dueOnMs = daysFromNow(20),
                description = "Field travel kit for the Pune-Bengaluru client circuit.",
                title = "Travel Kit",
                type = "Travel Petty Cash",
                colorSeed = "blue",
                txnPendingAmount = 300.0,
                txnAmountDistribution = mapOf(ReconBucket.MANAGER_APPROVAL_PENDING to 300.0),
            ),
            PettyCard(
                id = 2L,
                kitNo = "PC-1002",
                amount = 5000.0,
                balance = 1500.0,
                createdAtMs = daysAgo(25),
                dueOnMs = daysFromNow(5),
                description = "Client entertainment budget for Q3 renewals.",
                title = "Entertainment Kit",
                type = "Client Entertainment",
                colorSeed = "green",
                txnPendingAmount = 200.0,
                txnAmountDistribution = mapOf(ReconBucket.VOUCHER_NOT_CREATED to 200.0),
            ),
            PettyCard(
                id = 3L,
                kitNo = "PC-1003",
                amount = 5000.0,
                balance = 400.0,
                createdAtMs = daysAgo(55),
                dueOnMs = daysAgo(2),
                description = "Office supplies restock, nearly exhausted.",
                title = "Supplies Kit",
                type = "Office Supplies",
                colorSeed = "amber",
                txnPendingAmount = 0.0,
            ),
        )

    val pastPettyCards =
        listOf(
            PettyCard(
                id = 4L,
                kitNo = "PC-0900",
                amount = 2000.0,
                balance = 0.0,
                createdAtMs = daysAgo(120),
                dueOnMs = daysAgo(90),
                description = "Closed-out relocation kit, fully reconciled.",
                title = "Relocation Kit",
                type = "Travel Petty Cash",
                colorSeed = "blue",
                txnPendingAmount = 0.0,
            ),
        )

    private val pettyTxnStatuses = ReconBucket.entries

    fun pettyTransactions(cardId: Long): List<AdvanceTransaction> =
        (0 until 5).map { i ->
            AdvanceTransaction(
                id = cardId * 100L + i,
                title = "Petty spend #$i",
                amount = 50.0 + i * 25 + cardId * 5,
                dateMs = daysAgo(i * 4 + 1),
                voucherCreated = i % 2 == 0,
                status = pettyTxnStatuses[i % pettyTxnStatuses.size],
            )
        }

    val activeQrCards =
        listOf(
            QrCard(
                id = 1L,
                qrId = "QR-2001",
                title = "Fuel QR",
                description = "Fleet fuel top-ups, scan-to-pay at partner pumps.",
                balance = 800.0,
                cardLimit = 1000.0,
                total = 1000.0,
                validUntilMs = daysFromNow(90),
                colorSeed = "orange",
                isTransfer = false,
            ),
            QrCard(
                id = 2L,
                qrId = "QR-2002",
                title = "Field Ops QR",
                description = "Site-visit wallet, transferable between field engineers.",
                balance = 150.0,
                cardLimit = 500.0,
                total = 500.0,
                validUntilMs = daysFromNow(60),
                colorSeed = "purple",
                isTransfer = true,
            ),
        )

    val pastQrCards =
        listOf(
            QrCard(
                id = 3L,
                qrId = "QR-1899",
                title = "Expired Site QR",
                description = "Lapsed site-visit wallet from the last quarter.",
                balance = 0.0,
                cardLimit = 500.0,
                total = 500.0,
                validUntilMs = daysAgo(10),
                colorSeed = "purple",
                isTransfer = false,
            ),
        )

    fun qrTopUps(cardId: Long): List<AdvanceTransaction> =
        (0 until 3).map { i ->
            AdvanceTransaction(
                id = 500L + cardId * 100L + i,
                title = "Recharge #$i",
                amount = 100.0 + i * 50 + cardId * 5,
                dateMs = daysAgo(i * 10 + 3),
                voucherCreated = i != 1,
                status = ReconBucket.FINANCE_APPROVED,
            )
        }

    val openRequests =
        listOf(
            AdvanceRequest(
                id = 1L,
                title = "Travel Kit top-up",
                description = "Advance for the upcoming Pune-Bengaluru client visit.",
                amount = 4000.0,
                type = "Travel Petty Cash",
                section = AdvanceSection.PETTY,
                status = AdvanceRequestStatus.PENDING,
                createdAtMs = daysAgo(2),
                granters = listOf(Granter("Finance Desk", "Finance", GranterStatus.PENDING)),
            ),
            AdvanceRequest(
                id = 2L,
                title = "Field Ops QR recharge",
                description = "QR wallet top-up for the site-visit rotation.",
                amount = 500.0,
                type = "Site Visit QR",
                section = AdvanceSection.QR,
                status = AdvanceRequestStatus.APPROVAL,
                createdAtMs = daysAgo(1),
                granters = listOf(Granter("Asha Rao", "Manager", GranterStatus.PENDING)),
            ),
        )

    val closedRequests =
        listOf(
            AdvanceRequest(
                id = 3L,
                title = "Office Supplies restock",
                description = "Approved supplies kit for the new hire onboarding batch.",
                amount = 2000.0,
                type = "Office Supplies",
                section = AdvanceSection.PETTY,
                status = AdvanceRequestStatus.APPROVED,
                createdAtMs = daysAgo(30),
                granters = listOf(Granter("Finance Desk", "Finance", GranterStatus.APPROVED)),
            ),
            AdvanceRequest(
                id = 4L,
                title = "Fuel QR top-up",
                description = "Declined for missing trip justification.",
                amount = 1200.0,
                type = "Fuel QR",
                section = AdvanceSection.QR,
                status = AdvanceRequestStatus.DECLINED,
                createdAtMs = daysAgo(45),
                granters = listOf(Granter("Finance Desk", "Finance", GranterStatus.REJECTED)),
            ),
        )
}
