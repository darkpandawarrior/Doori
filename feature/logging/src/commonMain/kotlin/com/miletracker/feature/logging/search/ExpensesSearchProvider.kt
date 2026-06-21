package com.miletracker.feature.logging.search

import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchFilters
import com.miletracker.core.data.search.SearchProvider
import com.miletracker.core.data.search.SearchResult
import com.miletracker.core.data.search.SearchScope
import com.miletracker.feature.logging.repository.CardsTxnHistoryRepository
import com.miletracker.feature.logging.repository.ExpenseRepository
import com.miletracker.feature.logging.repository.SettlementHistoryRepository
import com.miletracker.feature.logging.repository.VoucherHistoryRepository

private const val DAY_MS = 86_400_000L

/**
 * SP.4: the logging/Spends module's contribution to master search (F0.5 registry). Searches expenses,
 * vouchers, settlements, and card-expense transactions; returns flat [SearchResult]s with deep links.
 * `feature:search` resolves this via `getAll<SearchProvider>()` with zero coupling back to this module.
 */
class ExpensesSearchProvider(
    private val expenses: ExpenseRepository,
    private val vouchers: VoucherHistoryRepository,
    private val settlements: SettlementHistoryRepository,
    private val cardTxns: CardsTxnHistoryRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> =
        setOf(SearchEntityType.TRANSACTION, SearchEntityType.VOUCHER, SearchEntityType.SETTLEMENT, SearchEntityType.CARD_TXN)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.EXPENSES && scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results = mutableListOf<SearchResult>()

        expenses.getAll()
            .filter { it.merchantName.contains(q, true) || it.id.contains(q, true) || it.category.label.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.TRANSACTION,
                        id = it.id,
                        title = it.merchantName,
                        subtitle = "${it.category.label} · ${it.status.name.lowercase()}",
                        status = it.status.name,
                        amount = it.amountRupees,
                        dateEpochDay = it.dateMs / DAY_MS,
                        deeplink = "miletracker://log/expense/${it.id}",
                    )
            }

        vouchers.vouchers()
            .filter { it.id.contains(q, true) || it.serviceTag.contains(q, true) || it.office.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.VOUCHER,
                        id = it.id,
                        title = it.id,
                        subtitle = "${it.serviceTag} · ${it.voucherState}",
                        status = it.voucherState,
                        amount = it.amount,
                        dateEpochDay = it.submittedOnMillis / DAY_MS,
                        deeplink = "miletracker://log/voucher/${it.id}",
                    )
            }

        settlements.settlements()
            .filter { it.id.contains(q, true) || it.periodLabel.contains(q, true) || it.method.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.SETTLEMENT,
                        id = it.id,
                        title = "${it.periodLabel} settlement",
                        subtitle = "${it.method} · ${it.status}",
                        status = it.status,
                        amount = it.amount,
                        dateEpochDay = it.settledOnMillis / DAY_MS,
                        deeplink = "miletracker://log/settlement/${it.id}",
                    )
            }

        cardTxns.transactions()
            .filter { it.id.contains(q, true) || it.merchant.contains(q, true) || it.category.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.CARD_TXN,
                        id = it.id,
                        title = it.merchant,
                        subtitle = "${it.category} · ${it.status}",
                        status = it.status,
                        amount = it.amount,
                        dateEpochDay = it.dateMillis / DAY_MS,
                        deeplink = "miletracker://log/cardtxn/${it.id}",
                    )
            }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
