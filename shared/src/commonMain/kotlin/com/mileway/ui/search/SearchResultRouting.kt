package com.mileway.ui.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchResult
import com.mileway.ui.AppGraph

/**
 * F0.5: maps a tapped [SearchResult] to the nav graph that owns its entity. Per-entity detail screens don't
 * all exist as standalone destinations yet, so a result opens its section's home graph (where the user lands
 * on the relevant history surface). Returns `null` for types with no destination wired (the tap is ignored).
 */
fun SearchResult.toSectionRoute(): String? =
    when (type) {
        SearchEntityType.QR -> AppGraph.PAYMENTS
        SearchEntityType.EVENT -> AppGraph.EVENTS
        SearchEntityType.TRIP, SearchEntityType.BOOKING -> AppGraph.TRAVEL
        SearchEntityType.MILEAGE, SearchEntityType.VOUCHER, SearchEntityType.TRANSACTION -> AppGraph.LOG
        SearchEntityType.INVOICE,
        SearchEntityType.PURCHASE_REQUEST,
        SearchEntityType.ASN,
        SearchEntityType.GIN,
        -> AppGraph.PAYABLES
        SearchEntityType.APPROVAL, SearchEntityType.CLARIFICATION -> AppGraph.APPROVALS
        SearchEntityType.CARD_TXN -> AppGraph.CARDS
        SearchEntityType.CHECKIN -> AppGraph.TRACK
        // PLAN_V29 P29.S.1: Advance now has a real provider — routes to Profile (where the advance
        // history/request screens live), matching its QuickActionRegistry deeplink.
        SearchEntityType.ADVANCE -> AppGraph.PROFILE
        SearchEntityType.SETTLEMENT,
        SearchEntityType.PARKING,
        -> null
    }
