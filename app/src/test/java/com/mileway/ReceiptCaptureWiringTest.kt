package com.mileway

import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.KeywordHeuristicClassifier
import com.mileway.core.ai.NoDocumentAiAnalyzer
import com.mileway.core.ai.TextRecognizer
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.DraftExpenseEntity
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.media.ocr.ReceiptHistorySource
import com.mileway.core.media.ocr.SavedReceipt
import com.mileway.core.media.ocr.analyzeReceiptCapture
import com.mileway.feature.tracking.repository.RecentReceiptHistory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReceiptCaptureWiringTest {
    private val intelligence =
        DocumentIntelligence(
            NoDocumentAiAnalyzer,
            object : TextRecognizer {
                override suspend fun recognize(image: String): String = "RECEIPT TOTAL 99.00"
            },
            KeywordHeuristicClassifier,
        )
    private val prompt = DocPrompt(DocType.RECEIPT, "Extract receipt", "{}")
    private val luma = IntArray(72) { 30 + it % 9 * 10 }

    @Test
    fun `capture seam passes saved candidates hashes and real time to intelligence`() =
        runTest {
            var queried = false
            val history =
                ReceiptHistorySource { since, until ->
                    queried = true
                    assertEquals(60_000L, since)
                    assertEquals(360_000L, until)
                    listOf(SavedReceipt("saved-receipt", "saved.jpg", 350_000L, "Different OCR", "12.50"))
                }
            val sampled = mutableListOf<String>()
            val result =
                analyzeReceiptCapture(intelligence, "rescan.jpg", prompt, 360_000L, history) { uri ->
                    sampled += uri
                    luma
                }
            assertTrue(queried)
            assertEquals(listOf("saved.jpg", "rescan.jpg"), sampled)
            val verdict = assertIs<DuplicateVerdict.Possible>(result.duplicate)
            assertEquals("saved-receipt", verdict.ref)
            assertTrue(verdict.reason.contains("image match"))
        }

    @Test
    fun `production history reads receipt query and saved draft inside window`() =
        runTest {
            val attachments = mockk<TripAttachmentDao>()
            coEvery { attachments.getRecentReceipts(60_000L, 360_000L) } returns
                listOf(
                    TripAttachmentEntity(id = 7L, trackToken = "trip", type = AttachmentType.RECEIPT, uri = "saved.jpg", createdAt = 350_000L),
                )
            val drafts = FakeDraftExpenseDao()
            drafts.upsertDraft(
                DraftExpenseEntity(
                    categoryName = null,
                    amountText = "12.50",
                    merchantName = "Cafe",
                    note = "",
                    receiptImagePath = "draft.jpg",
                    updatedAt = 340_000L,
                ),
            )
            val history = RecentReceiptHistory(attachments, drafts)
            val receipts = history.recent(60_000L, 360_000L)
            assertEquals(listOf("saved.jpg", "draft.jpg"), receipts.map { it.uri })
            val result = analyzeReceiptCapture(intelligence, "rescan.jpg", prompt, 360_000L, history) { luma }
            assertIs<DuplicateVerdict.Possible>(result.duplicate)
            coVerify(exactly = 2) { attachments.getRecentReceipts(60_000L, 360_000L) }
            drafts.upsertDraft(
                DraftExpenseEntity(
                    categoryName = null,
                    amountText = "12.50",
                    merchantName = "Cafe",
                    note = "",
                    receiptImagePath = "draft.jpg",
                    updatedAt = 59_999L,
                ),
            )
            assertEquals(listOf("saved.jpg"), history.recent(60_000L, 360_000L).map { it.uri })
        }
}
