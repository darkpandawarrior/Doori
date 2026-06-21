package com.miletracker

import com.miletracker.feature.cards.data.CardsMockDataProviderFactory
import com.miletracker.feature.cards.model.CardStatus
import com.miletracker.feature.cards.model.CardTxnClaimStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Q.2 — cards mock data layer (EN/AR/HI). */
class CardsMockDataTest {
    private val en = CardsMockDataProviderFactory.provider("en")

    @Test
    fun `english provider returns the virtual cards with one KYC-pending`() {
        val cards = en.virtualCards()
        assertEquals(4, cards.size)
        assertTrue(cards.any { it.status == CardStatus.KYC_PENDING && it.isKycPending })
        assertTrue(cards.any { it.isFrozen })
    }

    @Test
    fun `card types include a default and AI-suggested entries`() {
        val types = en.cardTypes()
        assertTrue(types.any { it.isDefault })
        assertTrue(types.any { it.isAiSuggested })
    }

    @Test
    fun `transactions span the claim lifecycle`() {
        val txns = en.transactions(1L)
        assertEquals(6, txns.size)
        val statuses = txns.map { it.claimStatus }.toSet()
        assertTrue(statuses.containsAll(setOf(CardTxnClaimStatus.UNCLAIMED, CardTxnClaimStatus.PERSONAL)))
    }

    @Test
    fun `requests include in-progress and approved`() {
        assertEquals(2, en.requests().size)
    }

    @Test
    fun `cardById resolves and locales differ`() {
        assertNotNull(en.cardById(1L))
        val ar = CardsMockDataProviderFactory.provider("ar")
        val hi = CardsMockDataProviderFactory.provider("hi")
        val enName = en.virtualCards().first().cardHolderName
        assertTrue(enName != ar.virtualCards().first().cardHolderName)
        assertTrue(enName != hi.virtualCards().first().cardHolderName)
    }
}
