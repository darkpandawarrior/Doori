package com.mileway.core.data.domain.payout

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PendingPaymentJournalTest {
    @Test
    fun simulatorReplayAfterRelaunchReturnsTheSameReceipt() {
        val pending = PendingPaymentJournal("report", 5000, "INR", createdAtMillis = 100)
        val receipt = SimulatedPayoutBackend().payout(pending)
        val restored = Json.decodeFromString<PendingPaymentJournal>(Json.encodeToString(pending))
        assertEquals(receipt, SimulatedPayoutBackend().payout(restored))
        assertEquals(receipt, SimulatedPayoutBackend().payout(receipt))
        assertEquals(PaymentStatus.PAID, receipt.status)
        assertFailsWith<IllegalArgumentException> { SimulatedPayoutBackend().payout(pending.copy(amountMinor = -1)) }
    }
}
