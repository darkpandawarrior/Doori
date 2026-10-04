package com.mileway.core.data.claim

import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRateSource

/** Matching attaches identity and FX provenance; the captured amount remains the split anchor. */
internal fun validateStatementMatch(
    original: ClaimLine,
    matched: ExpenseLine,
) {
    require(original is ExpenseLine && original.cardMatchId == null) { "Only an unmatched expense can be card matched" }
    require(original.amountMinor == matched.amountMinor && original.currency == matched.currency) { "A card match cannot change captured money" }
    require(
        original.copy(cardMatchId = matched.cardMatchId, fxRate = matched.fxRate, fxRatePinnedAt = matched.fxRatePinnedAt, splits = matched.splits) == matched,
    ) {
        "A card match cannot change other expense details"
    }
    if (matched.currency != "INR") {
        require(
            matched.fxRatePinnedAt != null && matched.fxRate?.source == FxRateSource.CARD_MATCHED && matched.fxRate?.isUsable(matched.currency, "INR") == true,
        ) {
            "Foreign card match requires a usable card FX pin"
        }
    }
}

/** Rejects attempts to change a matched line's captured amount, currency or transaction identity. */
internal fun validateCardAnchors(
    prior: List<ClaimLine>,
    updated: List<ClaimLine>,
) {
    prior.filter { it.cardMatchId != null }.forEach { line ->
        val next = updated.find { it.id == line.id }
        require(next == null || (next.amountMinor == line.amountMinor && next.currency == line.currency && next.cardMatchId == line.cardMatchId)) {
            "Card-matched amount and currency are locked"
        }
    }
}
