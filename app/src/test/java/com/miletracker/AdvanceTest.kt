package com.miletracker

import com.miletracker.feature.profile.model.CardStatus
import com.miletracker.feature.profile.model.CardType
import com.miletracker.feature.profile.repository.AdvanceRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdvanceTest {

    private val repo = AdvanceRepository()

    @Test
    fun `advanceRecords has 4 entries`() {
        assertEquals(4, repo.advanceRecords.size)
    }

    @Test
    fun `advance ids are unique`() {
        val ids = repo.advanceRecords.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `all advance amounts are positive`() {
        repo.advanceRecords.forEach { record ->
            assertTrue(record.amountRupees > 0.0, "${record.id} has non-positive amount")
        }
    }

    @Test
    fun `all statuses are distinct, tests cover multiple states`() {
        val statuses = repo.advanceRecords.map { it.status }.toSet()
        assertTrue(statuses.size >= 3, "Expected at least 3 distinct statuses")
    }

    @Test
    fun `cards has 2 entries`() {
        assertEquals(2, repo.cards.size)
    }

    @Test
    fun `cards contain exactly one VISA and one MASTERCARD`() {
        val types = repo.cards.map { it.cardType }.toSet()
        assertEquals(setOf(CardType.VISA, CardType.MASTERCARD), types)
    }

    @Test
    fun `cards contain both ACTIVE and BLOCKED status`() {
        val statuses = repo.cards.map { it.status }.toSet()
        assertEquals(setOf(CardStatus.ACTIVE, CardStatus.BLOCKED), statuses)
    }

    @Test
    fun `getCardById returns correct card`() {
        val card = repo.getCardById("CARD-001")
        assertNotNull(card)
        assertEquals("CARD-001", card.id)
        assertEquals(CardType.VISA, card.cardType)
        assertEquals(CardStatus.ACTIVE, card.status)
    }

    @Test
    fun `getCardById returns null for unknown id`() {
        assertNull(repo.getCardById("CARD-999"))
    }

    @Test
    fun `getTransactionsForCard returns transactions only for the given card`() {
        val txns = repo.getTransactionsForCard("CARD-001")
        assertTrue(txns.isNotEmpty(), "Expected transactions for CARD-001")
        assertTrue(txns.all { it.cardId == "CARD-001" })
    }

    @Test
    fun `getTransactionsForCard returns empty for CARD-002`() {
        val txns = repo.getTransactionsForCard("CARD-002")
        assertTrue(txns.isEmpty(), "Expected no transactions for blocked CARD-002")
    }

    @Test
    fun `active card has positive balance`() {
        val active = repo.cards.first { it.status == CardStatus.ACTIVE }
        assertTrue(active.balanceRupees > 0.0, "Active card should have positive balance")
    }

    @Test
    fun `all card last four digits are 4-character strings`() {
        repo.cards.forEach { card ->
            assertEquals(4, card.lastFourDigits.length, "${card.id} lastFourDigits should be 4 chars")
        }
    }

    @Test
    fun `data is deterministic across instances`() {
        val repo2 = AdvanceRepository()
        assertEquals(repo.advanceRecords.map { it.id }, repo2.advanceRecords.map { it.id })
        assertEquals(repo.cards.map { it.id }, repo2.cards.map { it.id })
    }
}
