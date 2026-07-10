package com.mileway.core.ai

import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import kotlin.math.abs

/**
 * Pure local heuristic: same merchant + amount within [windowMinutes] of an existing record →
 * a duplicate. No network/backend lookup — matches the "offline-first" contract everywhere else
 * in this repo.
 */
class DuplicateDetector(
    private val windowMinutes: Int = DEFAULT_WINDOW_MINUTES,
) {
    fun check(
        fields: Map<DocField, ExtractedValue>,
        timestampMillis: Long,
        candidates: List<DedupCandidate>,
    ): DuplicateVerdict {
        val merchant = fields[DocField.MERCHANT]?.value?.trim()?.lowercase()
        val total = fields[DocField.TOTAL]?.value?.trim()
        // Can't compare without both signals — never flag a duplicate on a partial read.
        if (merchant.isNullOrEmpty() || total.isNullOrEmpty()) return DuplicateVerdict.Unique

        val windowMillis = windowMinutes * MILLIS_PER_MINUTE
        val matches =
            candidates.filter { candidate ->
                candidate.merchant?.trim()?.lowercase() == merchant &&
                    candidate.total?.trim() == total &&
                    abs(timestampMillis - candidate.timestampMillis) <= windowMillis
            }
        if (matches.isEmpty()) return DuplicateVerdict.Unique

        val exact = matches.firstOrNull { it.timestampMillis == timestampMillis }
        if (exact != null) return DuplicateVerdict.Confirmed(exact.ref)

        val nearest = matches.minBy { abs(timestampMillis - it.timestampMillis) }
        return DuplicateVerdict.Possible(nearest.ref, "same merchant and amount within ${windowMinutes}min")
    }

    private companion object {
        const val DEFAULT_WINDOW_MINUTES = 5
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
