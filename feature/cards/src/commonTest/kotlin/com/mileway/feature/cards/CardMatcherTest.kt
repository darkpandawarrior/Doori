package com.mileway.feature.cards

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.feature.cards.import.StatementRow
import com.mileway.feature.cards.match.CardMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardMatcherTest {
    private val matcher = CardMatcher()
    private val row = StatementRow("tx1", "2026-09-25", "Cafe Orchard", 12000)

    private fun line(
        id: String = "line",
        date: String = "2026-09-25",
        merchant: String = "Cafe Orchard",
        amount: Long = 12000,
    ) = ExpenseLine(id, amount, "INR", merchant = merchant, category = "FOOD", incurredOn = date)

    @Test
    fun exactHit() {
        assertEquals("line", matcher.match(row, listOf(line())).line?.id)
    }

    @Test
    fun nearDateHit() {
        assertEquals("line", matcher.match(row, listOf(line(date = "2026-09-27"))).line?.id)
    }

    @Test
    fun merchantTypoHit() {
        assertEquals("line", matcher.match(row, listOf(line(merchant = "Cafe Orchrard"))).line?.id)
    }

    @Test
    fun amountMismatchDoesNotMatch() {
        assertNull(matcher.match(row, listOf(line(amount = 12500))).line)
    }

    @Test
    fun closerCandidateWinsRegardlessOfInputOrder() {
        val a = line("a", date = "2026-09-26", amount = 12050)
        val b = line("b")
        assertEquals("b", matcher.match(row, listOf(a, b)).line?.id)
        assertEquals("b", matcher.match(row, listOf(b, a)).line?.id)
        assertEquals("a", matcher.match(row, listOf(line("z"), line("a"))).line?.id)
    }

    @Test
    fun unrelatedOldOrAlreadyMatchedClaimsAreExcluded() {
        assertNull(matcher.match(row, listOf(line(date = "2026-09-20"), line(merchant = "Taxi"), line().copy(cardMatchId = "prior"))).line)
        assertNull(matcher.match(row, listOf(line().copy(incurredOn = null))).line)
    }

    @Test
    fun foreignAmountWithoutPinCannotBeComparedToInr() {
        val result = matcher.match(row, listOf(line(amount = 12000).copy(currency = "USD")))
        assertNull(result.line)
        assertEquals("Foreign amount check unavailable: no usable FX pin or original foreign amount", result.reason)
    }
}
