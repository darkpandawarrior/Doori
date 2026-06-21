package com.miletracker.ui.search

import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchResult
import com.miletracker.ui.AppGraph

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
        SearchEntityType.APPROVAL -> AppGraph.APPROVALS
        SearchEntityType.CARD_TXN -> AppGraph.CARDS
        SearchEntityType.CHECKIN -> AppGraph.TRACK
        SearchEntityType.SETTLEMENT,
        SearchEntityType.ADVANCE,
        SearchEntityType.PARKING,
        SearchEntityType.CLARIFICATION,
        -> null
    }
