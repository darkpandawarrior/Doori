package com.miletracker.core.data.search

/**
 * The kinds of entity master-search can return (F0.5). Each feature's `SearchProvider` declares which of
 * these it serves; the master-search UI groups results by type.
 */
enum class SearchEntityType {
    TRANSACTION,
    VOUCHER,
    SETTLEMENT,
    ADVANCE,
    EVENT,
    TRIP,
    BOOKING,
    INVOICE,
    PURCHASE_REQUEST,
    ASN,
    GIN,
    PARKING,
    CARD_TXN,
    QR,
    CLARIFICATION,
    MILEAGE,
    CHECKIN,
    APPROVAL,
}

/** Master-search scope tab — gates which entity types / quick actions are shown. */
enum class SearchScope { EXPENSES, PAYABLES, TRAVEL, VIEW_ALL }

/** One master-search hit (F0.5) — flat + platform-neutral so any provider can emit it. */
data class SearchResult(
    val type: SearchEntityType,
    val id: String,
    val title: String,
    val subtitle: String,
    val status: String? = null,
    val amount: Double? = null,
    val dateEpochDay: Long = 0L,
    val deeplink: String,
)

/** Active master-search filters (empty = unconstrained). */
data class SearchFilters(
    val types: Set<SearchEntityType> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val dateFromEpochDay: Long? = null,
    val dateToEpochDay: Long? = null,
)

/**
 * A feature module's contribution to master search (F0.5). Each feature binds its own `SearchProvider` into
 * Koin; `feature:search`'s repository resolves `getAll<SearchProvider>()` and fans out — so features never
 * depend on `feature:search` and new features light up in search for free.
 */
interface SearchProvider {
    /** The entity types this provider can return (used to skip it when filters exclude all of them). */
    val types: Set<SearchEntityType>

    suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult>
}
