package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InjectableDocumentAiAnalyzerTest {
    private val prompt = DocPrompt(DocType.RECEIPT, "instruction", "schema")

    @Test
    fun degradesWhenNothingInjected() =
        runTest {
            val seam = InjectableDocumentAiAnalyzer()

            assertFalse(seam.isAvailable())
            assertNull(seam.extract("uri", prompt))
        }

    @Test
    fun delegatesWhenInjectedAndAvailable() =
        runTest {
            val expected = AiExtraction(DocType.RECEIPT, emptyMap(), "raw", 0.9f)
            val seam = InjectableDocumentAiAnalyzer()
            seam.analyzer =
                object : DocumentAiAnalyzer {
                    override fun isAvailable() = true

                    override suspend fun extract(
                        image: String,
                        prompt: DocPrompt,
                    ) = expected
                }

            assertTrue(seam.isAvailable())
            assertEquals(expected, seam.extract("uri", prompt))
        }

    @Test
    fun degradesWhenInjectedButUnavailable() =
        runTest {
            val seam = InjectableDocumentAiAnalyzer()
            seam.analyzer =
                object : DocumentAiAnalyzer {
                    override fun isAvailable() = false

                    override suspend fun extract(
                        image: String,
                        prompt: DocPrompt,
                    ) = error("must not be called when unavailable")
                }

            assertFalse(seam.isAvailable())
            assertNull(seam.extract("uri", prompt))
        }
}
