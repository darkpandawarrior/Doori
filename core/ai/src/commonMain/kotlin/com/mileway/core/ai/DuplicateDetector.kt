package com.mileway.core.ai

import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import kotlin.math.abs

/** Local receipt comparison, with image matches adding evidence without excluding field matches. */
class DuplicateDetector(
    private val windowMinutes: Int = DEFAULT_WINDOW_MINUTES,
) {
    fun check(
        fields: Map<DocField, ExtractedValue>,
        timestampMillis: Long,
        candidates: List<DedupCandidate>,
        imageHash: Long? = null,
    ): DuplicateVerdict {
        val merchant = fields[DocField.MERCHANT]?.value?.trim()?.lowercase()
        val total = fields[DocField.TOTAL]?.value?.trim()
        val recent = candidates.filter { abs(timestampMillis - it.timestampMillis) <= windowMinutes * MILLIS_PER_MINUTE }

        fun fieldsMatch(candidate: DedupCandidate): Boolean =
            !merchant.isNullOrEmpty() &&
                !total.isNullOrEmpty() &&
                candidate.merchant?.trim()?.lowercase() == merchant &&
                candidate.total?.trim() == total

        val imageMatches =
            recent.filter { candidate ->
                imageHash != null && candidate.imageHash?.let { hammingDistance(imageHash, it) <= IMAGE_MATCH_THRESHOLD } == true
            }
        val imageMatch =
            imageMatches.firstOrNull { fieldsMatch(it) }
                ?: imageMatches.minByOrNull { abs(timestampMillis - it.timestampMillis) }
        if (imageMatch != null) {
            return if (fieldsMatch(imageMatch)) {
                DuplicateVerdict.Confirmed(imageMatch.ref)
            } else {
                DuplicateVerdict.Possible(imageMatch.ref, "receipt image match within ${windowMinutes}min")
            }
        }

        val fieldMatches = recent.filter { fieldsMatch(it) }
        fieldMatches.firstOrNull { it.timestampMillis == timestampMillis }?.let { return DuplicateVerdict.Confirmed(it.ref) }
        val nearest = fieldMatches.minByOrNull { abs(timestampMillis - it.timestampMillis) } ?: return DuplicateVerdict.Unique
        return DuplicateVerdict.Possible(nearest.ref, "same merchant and amount within ${windowMinutes}min")
    }

    companion object {
        const val DEFAULT_WINDOW_MINUTES = 5
        const val MILLIS_PER_MINUTE = 60_000L
        const val WINDOW_MILLIS = DEFAULT_WINDOW_MINUTES * MILLIS_PER_MINUTE
        const val IMAGE_MATCH_THRESHOLD = 8

        /** Number of differing bits in two 64-bit receipt dHashes. */
        fun hammingDistance(
            first: Long,
            second: Long,
        ): Int = (first xor second).countOneBits()
    }
}
