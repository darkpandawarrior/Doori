package com.miletracker.feature.payables.search

import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchFilters
import com.miletracker.core.data.search.SearchProvider
import com.miletracker.core.data.search.SearchResult
import com.miletracker.core.data.search.SearchScope
import com.miletracker.feature.payables.model.PayablesDoc
import com.miletracker.feature.payables.model.PayablesDocType
import com.miletracker.feature.payables.repository.PayablesHistoryRepository

private const val DAY_MS = 86_400_000L

/**
 * PB.5: the payables module's contribution to master search (F0.5 registry). Searches the unified payables
 * history (Invoice / PR / GIN / Park In-Out / ASN); returns flat [SearchResult]s with deep links.
 * `feature:search` resolves this via `getAll<SearchProvider>()` with zero coupling back to this module.
 */
class PayablesSearchProvider(
    private val history: PayablesHistoryRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> =
        setOf(
            SearchEntityType.INVOICE,
            SearchEntityType.PURCHASE_REQUEST,
            SearchEntityType.GIN,
            SearchEntityType.PARKING,
            SearchEntityType.ASN,
        )

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.PAYABLES && scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results =
            history.documents()
                .filter {
                    it.id.contains(q, true) || it.title.contains(q, true) || it.reference.contains(q, true)
                }
                .map { doc ->
                    SearchResult(
                        type = entityTypeFor(doc.type),
                        id = doc.id,
                        title = "${doc.type.label} · ${doc.title}",
                        subtitle = "${doc.reference} · ${doc.status.label}",
                        status = doc.status.label,
                        amount = doc.amount,
                        dateEpochDay = doc.dateMillis / DAY_MS,
                        deeplink = deeplinkFor(doc),
                    )
                }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }

    private fun entityTypeFor(type: PayablesDocType): SearchEntityType =
        when (type) {
            PayablesDocType.INVOICE -> SearchEntityType.INVOICE
            PayablesDocType.PURCHASE_REQUEST -> SearchEntityType.PURCHASE_REQUEST
            PayablesDocType.GIN -> SearchEntityType.GIN
            PayablesDocType.PARK_IN_OUT -> SearchEntityType.PARKING
            PayablesDocType.ASN -> SearchEntityType.ASN
        }

    private fun deeplinkFor(doc: PayablesDoc): String {
        val segment =
            when (doc.type) {
                PayablesDocType.INVOICE -> "invoice"
                PayablesDocType.PURCHASE_REQUEST -> "pr"
                PayablesDocType.GIN -> "gin"
                PayablesDocType.PARK_IN_OUT -> "parking"
                PayablesDocType.ASN -> "asn"
            }
        return "miletracker://payables/$segment/${doc.id}"
    }
}
