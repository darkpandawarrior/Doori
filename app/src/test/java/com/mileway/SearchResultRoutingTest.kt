package com.mileway

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchResult
import com.mileway.ui.AppGraph
import com.mileway.ui.search.toSectionRoute
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** F0.5: search-result → section-graph mapping used when a result row is tapped. */
class SearchResultRoutingTest {

    private fun resultOf(type: SearchEntityType) =
        SearchResult(type = type, id = "x", title = "t", subtitle = "s", deeplink = "mileway://x")

    @Test
    fun `payments QR routes to the payments graph`() {
        assertEquals(AppGraph.PAYMENTS, resultOf(SearchEntityType.QR).toSectionRoute())
    }

    @Test
    fun `trips and bookings route to travel`() {
        assertEquals(AppGraph.TRAVEL, resultOf(SearchEntityType.TRIP).toSectionRoute())
        assertEquals(AppGraph.TRAVEL, resultOf(SearchEntityType.BOOKING).toSectionRoute())
    }

    @Test
    fun `payables document types route to payables`() {
        assertEquals(AppGraph.PAYABLES, resultOf(SearchEntityType.INVOICE).toSectionRoute())
        assertEquals(AppGraph.PAYABLES, resultOf(SearchEntityType.PURCHASE_REQUEST).toSectionRoute())
    }

    @Test
    fun `types without a wired destination map to null`() {
        assertNull(resultOf(SearchEntityType.SETTLEMENT).toSectionRoute())
        assertNull(resultOf(SearchEntityType.PARKING).toSectionRoute())
    }

    @Test
    fun `the 5 newly-bound providers all route somewhere real`() {
        assertEquals(AppGraph.LOG, resultOf(SearchEntityType.MILEAGE).toSectionRoute())
        assertEquals(AppGraph.TRACK, resultOf(SearchEntityType.CHECKIN).toSectionRoute())
        assertEquals(AppGraph.APPROVALS, resultOf(SearchEntityType.APPROVAL).toSectionRoute())
        assertEquals(AppGraph.APPROVALS, resultOf(SearchEntityType.CLARIFICATION).toSectionRoute())
        assertEquals(AppGraph.PROFILE, resultOf(SearchEntityType.ADVANCE).toSectionRoute())
    }
}
