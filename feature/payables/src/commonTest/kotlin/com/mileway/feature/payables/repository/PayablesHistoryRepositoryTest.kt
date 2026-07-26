package com.mileway.feature.payables.repository

import com.mileway.feature.payables.model.PayablesDocStatus
import com.mileway.feature.payables.model.PayablesDocType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private const val NOW_MS = 1_700_000_000_000L

private class FixedClock(private val ms: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
}

/** PB.4: covers [PayablesHistoryRepository] — the type/status filter combinations and newest-first sort. */
class PayablesHistoryRepositoryTest {
    private fun repo() = PayablesHistoryRepository(FixedClock(NOW_MS))

    @Test
    fun `documents returns all twelve seeded docs sorted newest first`() {
        val docs = repo().documents()

        assertEquals(12, docs.size)
        // PARK_IN_OUT MH12 is 0 days old — the single newest doc.
        assertEquals("PRK-9008", docs.first().id)
        // INVOICE BlueOak is 17 days old — the oldest doc.
        assertEquals("INV-9002", docs.last().id)
        assertTrue(docs.zipWithNext().all { (a, b) -> a.dateMillis >= b.dateMillis })
    }

    @Test
    fun `documents filters by type only`() {
        val invoices = repo().documents(type = PayablesDocType.INVOICE)

        assertEquals(3, invoices.size)
        assertTrue(invoices.all { it.type == PayablesDocType.INVOICE })
        assertEquals(listOf("INV-9000", "INV-9001", "INV-9002"), invoices.map { it.id })
    }

    @Test
    fun `documents filters by status only`() {
        val pending = repo().documents(status = PayablesDocStatus.PENDING)

        assertEquals(4, pending.size)
        assertTrue(pending.all { it.status == PayablesDocStatus.PENDING })
    }

    @Test
    fun `documents filters by type and status combined`() {
        val result = repo().documents(type = PayablesDocType.GIN, status = PayablesDocStatus.PENDING)

        assertEquals(listOf("GIN-9007"), result.map { it.id })
    }

    @Test
    fun `documents returns empty for a type-status combo that does not exist`() {
        // No INVOICE is ever DRAFT in the seed spec.
        val result = repo().documents(type = PayablesDocType.INVOICE, status = PayablesDocStatus.DRAFT)

        assertTrue(result.isEmpty())
    }
}
