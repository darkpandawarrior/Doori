package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentImageRef
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeAiAnalyzer(
    private val available: Boolean,
    private val extraction: AiExtraction? = null,
) : DocumentAiAnalyzer {
    var extractCalled = false
        private set

    override fun isAvailable(): Boolean = available

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? {
        extractCalled = true
        return extraction
    }
}

private class FakeTextRecognizer(private val text: String) : TextRecognizer {
    override suspend fun recognize(image: DocumentImageRef): String = text
}

private val PROMPT = DocPrompt(DocType.RECEIPT, "extract the receipt fields", "{merchant,total}")

class DocumentIntelligenceTest {
    @Test
    fun `degrade path - AI unavailable still returns a full result from text and heuristic`() =
        runTest {
            val ai = FakeAiAnalyzer(available = false)
            val text = FakeTextRecognizer("RECEIPT\nSubtotal 10.00\nCash tendered 20.00\nChange due 10.00")
            val intelligence = DocumentIntelligence(ai, text, KeywordHeuristicClassifier)

            val result = intelligence.analyze("content://image", PROMPT)

            assertFalse(ai.extractCalled, "isAvailable()==false must short-circuit extract()")
            assertEquals(DocType.RECEIPT, result.docType)
            assertFalse(AnalyzerSource.ON_DEVICE_AI in result.contributingSources)
            assertTrue(AnalyzerSource.TEXT_RECOGNITION in result.contributingSources)
            assertTrue(AnalyzerSource.HEURISTIC_CLASSIFIER in result.contributingSources)
            assertEquals(DuplicateVerdict.Unique, result.duplicate)
        }

    @Test
    fun `AI available and confident wins over the degrade path`() =
        runTest {
            val extraction =
                AiExtraction(
                    docType = DocType.INVOICE,
                    fields = mapOf(DocField.MERCHANT to ExtractedValue("Acme Corp", 0.95f, AnalyzerSource.ON_DEVICE_AI)),
                    rawText = "TAX INVOICE Acme Corp",
                    confidence = 0.95f,
                )
            val ai = FakeAiAnalyzer(available = true, extraction = extraction)
            val text = FakeTextRecognizer("TAX INVOICE Acme Corp")
            val intelligence = DocumentIntelligence(ai, text, KeywordHeuristicClassifier)

            val result = intelligence.analyze("content://image", PROMPT)

            assertTrue(ai.extractCalled)
            assertEquals(DocType.INVOICE, result.docType)
            assertEquals("Acme Corp", result.fields.getValue(DocField.MERCHANT).value)
            assertTrue(AnalyzerSource.ON_DEVICE_AI in result.contributingSources)
            assertTrue(result.overallConfidence > 0f)
        }

    @Test
    fun `AI available result has higher confidence than the degrade path for the same document`() =
        runTest {
            val rawText = "RECEIPT\nTotal 12.50\nSubtotal 10.00\nChange due 2.50"
            val degraded =
                DocumentIntelligence(FakeAiAnalyzer(available = false), FakeTextRecognizer(rawText), KeywordHeuristicClassifier)
                    .analyze("img", PROMPT)

            val extraction =
                AiExtraction(
                    docType = DocType.RECEIPT,
                    fields = mapOf(DocField.TOTAL to ExtractedValue("12.50", 0.97f, AnalyzerSource.ON_DEVICE_AI)),
                    rawText = rawText,
                    confidence = 0.97f,
                )
            val aiAssisted =
                DocumentIntelligence(
                    FakeAiAnalyzer(available = true, extraction = extraction),
                    FakeTextRecognizer(rawText),
                    KeywordHeuristicClassifier,
                ).analyze("img", PROMPT)

            assertTrue(
                aiAssisted.overallConfidence > degraded.overallConfidence,
                "AI-assisted confidence (${aiAssisted.overallConfidence}) should exceed degrade-path " +
                    "confidence (${degraded.overallConfidence})",
            )
        }

    @Test
    fun `duplicate candidates flow through to the final result`() =
        runTest {
            val rawText = "RECEIPT\nCafe Roma\nTotal 12.50"
            val ai = FakeAiAnalyzer(available = false)
            val text = FakeTextRecognizer(rawText)
            val intelligence = DocumentIntelligence(ai, text, KeywordHeuristicClassifier)

            // No MERCHANT field is extracted by the raw-regex tier (by design), so even a
            // matching candidate can't be flagged as a duplicate from the degrade path alone.
            val candidates = listOf(DedupCandidate("ref-1", "Cafe Roma", "12.50", 0L))
            val result = intelligence.analyze("img", PROMPT, dedupCandidates = candidates, timestampMillis = 0L)

            assertEquals(DuplicateVerdict.Unique, result.duplicate)
        }
}
