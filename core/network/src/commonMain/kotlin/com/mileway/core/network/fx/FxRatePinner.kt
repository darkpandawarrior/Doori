package com.mileway.core.network.fx

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import kotlin.time.Clock

/** Card rate wins, then an existing pin, ECB reference, and finally an approximate manual rate. */
class FxRatePinner(
    private val reference: suspend (String, String, String?) -> FxRate? = { _, _, _ -> null },
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    /** Returns an unchanged foreign amount and a saved pin, or an explicitly unresolved line. */
    suspend fun pin(
        line: ExpenseLine,
        sourceDate: String? = null,
        cardRate: FxRate? = null,
        manualRate: Double? = null,
    ): ExpenseLine {
        if (line.currency == "INR") return line.copy(fxRate = null, fxRatePinnedAt = null)
        val card = cardRate?.takeIf { line.cardMatchId != null && it.source == FxRateSource.CARD_MATCHED && it.isUsable(line.currency, "INR") }
        if (card == null && line.fxRatePinnedAt != null && line.fxRate?.isUsable(line.currency, "INR") == true) return line
        val rate = card
            ?: reference(line.currency, "INR", sourceDate)?.takeIf {
                it.source == FxRateSource.ECB_REFERENCE && it.isUsable(line.currency, "INR")
            }
            ?: manualRate?.let { FxRate(it, line.currency, source = FxRateSource.MANUAL_APPROXIMATE) }
                ?.takeIf { it.isUsable(line.currency, "INR") }
        return line.copy(fxRate = rate, fxRatePinnedAt = rate?.let { nowMillis() })
    }
}
