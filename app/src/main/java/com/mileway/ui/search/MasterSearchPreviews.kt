package com.mileway.ui.search

import androidx.compose.runtime.Composable
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchResult
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.previews.PreviewSurface

/** Deterministic sample result set used by the master-search previews / screenshot catalog. */
private fun sampleGroups(): List<SearchResultGroup> =
    listOf(
        SearchResultGroup(
            type = SearchEntityType.TRIP,
            results =
                listOf(
                    SearchResult(SearchEntityType.TRIP, "t1", "Goa offsite", "Mar 12 · 248 km", status = "Submitted", dateEpochDay = 20_160, deeplink = "mileway://travel/t1"),
                    SearchResult(SearchEntityType.TRIP, "t2", "Client visit · Pune", "Mar 09 · 41 km", status = "Approved", dateEpochDay = 20_157, deeplink = "mileway://travel/t2"),
                ),
        ),
        SearchResultGroup(
            type = SearchEntityType.QR,
            results =
                listOf(
                    SearchResult(SearchEntityType.QR, "p1", "Paid · Chai Point", "UPI · Reimbursable", status = "Completed", amount = 240.0, dateEpochDay = 20_158, deeplink = "mileway://payments/p1"),
                ),
        ),
        SearchResultGroup(
            type = SearchEntityType.EVENT,
            results =
                listOf(
                    SearchResult(SearchEntityType.EVENT, "e1", "Quarterly summit", "Bengaluru · 3 days", status = "Upcoming", dateEpochDay = 20_170, deeplink = "mileway://events/e1"),
                ),
        ),
    )

private val previewTypes =
    listOf(SearchEntityType.TRIP, SearchEntityType.QR, SearchEntityType.EVENT, SearchEntityType.BOOKING, SearchEntityType.VOUCHER)

/** Master search with grouped results across trips, payments and events. */
@Composable
fun PreviewMasterSearchResults() {
    PreviewSurface {
        MasterSearchContent(
            state =
                MasterSearchUiState(
                    query = "goa",
                    scopeIndex = 0,
                    results = ScreenState.Content(sampleGroups()),
                ),
            availableTypes = previewTypes,
            onBack = {},
            onQueryChange = {},
            onSelectScope = {},
            onToggleType = {},
            onClearFilters = {},
            onResultClick = {},
        )
    }
}

/** Master search empty / resting state (no query yet). */
@Composable
fun PreviewMasterSearchEmpty() {
    PreviewSurface {
        MasterSearchContent(
            state = MasterSearchUiState(query = "", results = ScreenState.Content(emptyList())),
            availableTypes = previewTypes,
            onBack = {},
            onQueryChange = {},
            onSelectScope = {},
            onToggleType = {},
            onClearFilters = {},
            onResultClick = {},
        )
    }
}
