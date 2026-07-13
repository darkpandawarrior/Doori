package com.mileway.core.data.search

/**
 * F0.5 / PLAN_V29 P29.S.2: pure ID-pattern auto-detection — matches a typed query against the id
 * shapes providers already generate (e.g. `EXP-1234` → TRANSACTION) so the UI can surface a
 * "Searching as <Type>" hint without a network round-trip. No-op (empty set) for free-text queries
 * that don't look like an id.
 */
object IdPatternMatcher {
    // Anchored at the start only (not the full id) so a hint appears while the user is still
    // mid-typing the numeric suffix, not just once the whole id is complete.
    private val patterns: List<Pair<Regex, SearchEntityType>> =
        listOf(
            Regex("^EXP-", RegexOption.IGNORE_CASE) to SearchEntityType.TRANSACTION,
            Regex("^ADV-", RegexOption.IGNORE_CASE) to SearchEntityType.ADVANCE,
            Regex("^INV-", RegexOption.IGNORE_CASE) to SearchEntityType.INVOICE,
            Regex("^PO-", RegexOption.IGNORE_CASE) to SearchEntityType.PURCHASE_REQUEST,
            Regex("^GIN-", RegexOption.IGNORE_CASE) to SearchEntityType.GIN,
            Regex("^ASN-", RegexOption.IGNORE_CASE) to SearchEntityType.ASN,
            Regex("^STL-", RegexOption.IGNORE_CASE) to SearchEntityType.SETTLEMENT,
            Regex("^VCH-", RegexOption.IGNORE_CASE) to SearchEntityType.VOUCHER,
            Regex("^TRP-", RegexOption.IGNORE_CASE) to SearchEntityType.TRIP,
            Regex("^BK\\d", RegexOption.IGNORE_CASE) to SearchEntityType.BOOKING,
            Regex("^EVT-", RegexOption.IGNORE_CASE) to SearchEntityType.EVENT,
            Regex("^TXN-", RegexOption.IGNORE_CASE) to SearchEntityType.CARD_TXN,
            Regex("^CARD-", RegexOption.IGNORE_CASE) to SearchEntityType.CARD_TXN,
            // Approvals use a bare "A" + 3 digits (A001..A0xx) — the `\d` after "A" keeps this from
            // ever firing on "ASN-..." or "ADV-...", which are letters after the "A".
            Regex("^A\\d", RegexOption.IGNORE_CASE) to SearchEntityType.APPROVAL,
        )

    /** Every [SearchEntityType] whose known id shape [query] (trimmed) currently matches. */
    fun detect(query: String): Set<SearchEntityType> {
        val q = query.trim()
        if (q.isEmpty()) return emptySet()
        return patterns.filter { (regex, _) -> regex.containsMatchIn(q) }.mapTo(linkedSetOf()) { it.second }
    }
}
