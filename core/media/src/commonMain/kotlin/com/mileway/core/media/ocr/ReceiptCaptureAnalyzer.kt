package com.mileway.core.media.ocr

import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.DuplicateDetector
import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentAnalysis

/** Saved receipt data supplied by the persistence owner, without a media to data dependency. */
data class SavedReceipt(
    val ref: String,
    val uri: String,
    val timestampMillis: Long,
    val merchant: String? = null,
    val total: String? = null,
)

/** Reads receipts saved inside an inclusive capture-time window. */
fun interface ReceiptHistorySource {
    suspend fun recent(
        sinceMillis: Long,
        untilMillis: Long,
    ): List<SavedReceipt>
}

/** Shared capture seam: reads real history and computes hashes before the duplicate verdict. */
suspend fun analyzeReceiptCapture(
    intelligence: DocumentIntelligence,
    uri: String,
    prompt: DocPrompt,
    timestampMillis: Long,
    history: ReceiptHistorySource,
    sampler: suspend (String) -> IntArray? = ::sampleReceiptLuma,
): DocumentAnalysis {
    val candidates =
        history.recent(timestampMillis - DuplicateDetector.WINDOW_MILLIS, timestampMillis).map { receipt ->
            DedupCandidate(
                ref = receipt.ref,
                merchant = receipt.merchant,
                total = receipt.total,
                timestampMillis = receipt.timestampMillis,
                imageHash = sampler(receipt.uri)?.let { ReceiptDedupHasher.hash(it) },
            )
        }
    return intelligence.analyze(
        image = uri,
        prompt = prompt,
        dedupCandidates = candidates,
        timestampMillis = timestampMillis,
        imageHash = sampler(uri)?.let { ReceiptDedupHasher.hash(it) },
    )
}
