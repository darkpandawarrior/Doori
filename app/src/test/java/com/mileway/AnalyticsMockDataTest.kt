package com.mileway

import com.mileway.stub.AnalyticsMockData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalyticsMockDataTest {

    @Test
    fun `dailySeries has exactly 30 entries`() {
        assertEquals(30, AnalyticsMockData.dailySeries.size)
    }

    @Test
    fun `weeklySeries has exactly 7 entries`() {
        assertEquals(7, AnalyticsMockData.weeklySeries.size)
    }

    @Test
    fun `weeklySeries is the last 7 entries of dailySeries`() {
        assertEquals(
            AnalyticsMockData.dailySeries.takeLast(7),
            AnalyticsMockData.weeklySeries
        )
    }

    @Test
    fun `totalSpend equals sum of categoryTotals`() {
        val expected = AnalyticsMockData.categoryTotals.values.sum()
        assertEquals(expected, AnalyticsMockData.totalSpend, 0.01)
    }

    @Test
    fun `categoryTotals covers exactly four categories`() {
        val keys = AnalyticsMockData.categoryTotals.keys
        assertEquals(setOf("Mileage", "Expense", "Travel", "Advance"), keys)
    }

    @Test
    fun `all categoryTotals are positive`() {
        AnalyticsMockData.categoryTotals.values.forEach { amount ->
            assertTrue(amount > 0.0, "Expected positive amount, got $amount")
        }
    }

    @Test
    fun `topMerchants has 5 entries`() {
        assertEquals(5, AnalyticsMockData.topMerchants.size)
    }

    @Test
    fun `recentActivity has 5 entries`() {
        assertEquals(5, AnalyticsMockData.recentActivity.size)
    }

    @Test
    fun `quickInsights is non-empty`() {
        assertTrue(AnalyticsMockData.quickInsights.isNotEmpty())
    }

    @Test
    fun `compliancePercent is within 0-100 range`() {
        val pct = AnalyticsMockData.compliancePercent
        assertTrue(pct in 0..100, "compliancePercent=$pct out of range")
    }

    @Test
    fun `dailySeries is deterministic across accesses`() {
        val first = AnalyticsMockData.dailySeries.map { it.amountRupees }
        val second = AnalyticsMockData.dailySeries.map { it.amountRupees }
        assertEquals(first, second)
    }

    @Test
    fun `all daily amounts are positive`() {
        AnalyticsMockData.dailySeries.forEachIndexed { i, day ->
            assertTrue(day.amountRupees > 0.0, "Day $i has non-positive amount ${day.amountRupees}")
        }
    }

    @Test
    fun `seriesForCategory returns 30 entries for every known category`() {
        listOf("Mileage", "Expense", "Travel", "Advance").forEach { cat ->
            val series = AnalyticsMockData.seriesForCategory(cat)
            assertEquals(30, series.size, "Expected 30 entries for category $cat")
        }
    }

    @Test
    fun `seriesForCategory amounts are scaled below original`() {
        val full = AnalyticsMockData.dailySeries.sumOf { it.amountRupees }
        listOf("Mileage", "Expense", "Travel", "Advance").forEach { cat ->
            val scaled = AnalyticsMockData.seriesForCategory(cat).sumOf { it.amountRupees }
            assertTrue(scaled < full, "Scaled total for $cat should be less than full total")
        }
    }

    @Test
    fun `merchantsForCategory returns non-empty list for known categories`() {
        listOf("Mileage", "Expense", "Travel", "Advance").forEach { cat ->
            val merchants = AnalyticsMockData.merchantsForCategory(cat)
            assertTrue(merchants.isNotEmpty(), "Expected non-empty merchants for $cat")
        }
    }

    @Test
    fun `recentActivity items have non-blank titles and categories`() {
        AnalyticsMockData.recentActivity.forEach { item ->
            assertTrue(item.title.isNotBlank(), "title is blank")
            assertTrue(item.category.isNotBlank(), "category is blank")
        }
    }
}
