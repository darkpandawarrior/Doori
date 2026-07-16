package com.mileway.feature.advances.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AdvanceModelsTest {
    @Test
    fun `balance at or above 50 percent of total is ACTIVE`() {
        assertEquals(CardHealth.ACTIVE, cardHealth(balance = 500.0, total = 1000.0))
        assertEquals(CardHealth.ACTIVE, cardHealth(balance = 1000.0, total = 1000.0))
    }

    @Test
    fun `balance from 20 up to (not including) 50 percent is LOW_BALANCE`() {
        assertEquals(CardHealth.LOW_BALANCE, cardHealth(balance = 200.0, total = 1000.0))
        assertEquals(CardHealth.LOW_BALANCE, cardHealth(balance = 499.0, total = 1000.0))
    }

    @Test
    fun `balance below 20 percent is CRITICAL`() {
        assertEquals(CardHealth.CRITICAL, cardHealth(balance = 199.0, total = 1000.0))
        assertEquals(CardHealth.CRITICAL, cardHealth(balance = 0.0, total = 1000.0))
    }

    @Test
    fun `a zero or negative total is CRITICAL regardless of balance`() {
        assertEquals(CardHealth.CRITICAL, cardHealth(balance = 0.0, total = 0.0))
        assertEquals(CardHealth.CRITICAL, cardHealth(balance = 500.0, total = -100.0))
    }

    @Test
    fun `PettyCard totalBalance sums balance and pending transaction amount`() {
        val card =
            PettyCard(
                id = 1L, kitNo = "PC-1", amount = 1000.0, balance = 300.0, createdAtMs = 0L, dueOnMs = 0L,
                description = "d", title = "t", type = "Travel", colorSeed = "blue", txnPendingAmount = 50.0,
            )
        assertEquals(350.0, card.totalBalance)
    }

    @Test
    fun `displayStatus maps PENDING and APPROVAL to their reference-app copy`() {
        assertEquals("FINANCE PENDING", AdvanceRequestStatus.PENDING.displayStatus())
        assertEquals("MANAGER PENDING", AdvanceRequestStatus.APPROVAL.displayStatus())
        assertEquals("APPROVED", AdvanceRequestStatus.APPROVED.displayStatus())
    }

    @Test
    fun `displayStatus forks DECLINED on whether an admin declined it`() {
        assertEquals("FINANCE DECLINED", AdvanceRequestStatus.DECLINED.displayStatus(declinedByAdmin = true))
        assertEquals("DECLINED", AdvanceRequestStatus.DECLINED.displayStatus(declinedByAdmin = false))
    }
}
