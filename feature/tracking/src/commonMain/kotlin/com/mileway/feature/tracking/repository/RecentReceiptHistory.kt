package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.DraftExpenseDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.media.ocr.ReceiptHistorySource
import com.mileway.core.media.ocr.SavedReceipt

/** Reads the receipt stores written by trip attachments and the expense SaveDraft action. */
class RecentReceiptHistory(
    private val attachments: TripAttachmentDao,
    private val drafts: DraftExpenseDao,
) : ReceiptHistorySource {
    override suspend fun recent(sinceMillis: Long, untilMillis: Long): List<SavedReceipt> {
        val receipts = attachments.getRecentReceipts(sinceMillis, untilMillis).map { row ->
            SavedReceipt("attachment:${row.id}", row.uri, row.createdAt)
        }
        val draft = drafts.getDraft()
        val draftReceipt = draft?.receiptImagePath?.takeIf { draft.updatedAt in sinceMillis..untilMillis }?.let { uri ->
            SavedReceipt("draft:${draft.draftId}", uri, draft.updatedAt, draft.merchantName, draft.amountText)
        }
        return receipts + listOfNotNull(draftReceipt)
    }
}
