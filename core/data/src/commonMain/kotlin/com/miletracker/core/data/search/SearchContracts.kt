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

/** Human-readable group label for a [SearchEntityType] (used as the master-search section header). */
val SearchEntityType.displayLabel: String
    get() =
        when (this) {
            SearchEntityType.TRANSACTION -> "Transactions"
            SearchEntityType.VOUCHER -> "Vouchers"
            SearchEntityType.SETTLEMENT -> "Settlements"
            SearchEntityType.ADVANCE -> "Advances"
            SearchEntityType.EVENT -> "Events"
            SearchEntityType.TRIP -> "Trips"
            SearchEntityType.BOOKING -> "Bookings"
            SearchEntityType.INVOICE -> "Invoices"
            SearchEntityType.PURCHASE_REQUEST -> "Purchase requests"
            SearchEntityType.ASN -> "Shipments"
            SearchEntityType.GIN -> "Goods receipts"
            SearchEntityType.PARKING -> "Parking"
            SearchEntityType.CARD_TXN -> "Card transactions"
            SearchEntityType.QR -> "Payments"
            SearchEntityType.CLARIFICATION -> "Clarifications"
            SearchEntityType.MILEAGE -> "Mileage"
            SearchEntityType.CHECKIN -> "Check-ins"
            SearchEntityType.APPROVAL -> "Approvals"
        }

/** Master-search scope tab, gates which entity types / quick actions are shown. */
enum class SearchScope { EXPENSES, PAYABLES, TRAVEL, VIEW_ALL }

/** Human-readable label for a [SearchScope] tab. */
val SearchScope.displayLabel: String
    get() =
        when (this) {
            SearchScope.VIEW_ALL -> "All"
            SearchScope.EXPENSES -> "Expenses"
            SearchScope.PAYABLES -> "Payables"
            SearchScope.TRAVEL -> "Travel"
        }

/** One master-search hit (F0.5), flat + platform-neutral so any provider can emit it. */
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
 * Koin; `feature:search`'s repository resolves `getAll<SearchProvider>()` and fans out, so features never
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
