package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.agent.engine.AssistantChunk
import com.miletracker.feature.agent.engine.llm.LlmAssistantEngine
import com.miletracker.feature.agent.engine.llm.LlmGateway
import com.miletracker.feature.agent.engine.llm.NoOpLlmGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmAssistantEngineTest {

    @Test
    fun `streams tokens from gateway and finalises with Done`() = runTest {
        val fakeGateway = object : LlmGateway {
            override fun stream(prompt: String): Flow<String> = flowOf("Hello", " world")
        }
        val engine = LlmAssistantEngine(fakeGateway)
        var streamedText = ""
        var doneText = ""
        engine.respond("conv-1", "hi", 0).test {
            assertTrue(awaitItem() is AssistantChunk.Thinking)
            while (true) {
                when (val item = awaitItem()) {
                    is AssistantChunk.Token -> streamedText += item.text
                    is AssistantChunk.Done -> { doneText = item.fullText; break }
                    else -> {}
                }
            }
            awaitComplete()
        }
        assertEquals("Hello world", streamedText)
        assertEquals("Hello world", doneText)
    }

    @Test
    fun `NoOpLlmGateway emits empty flow`() = runTest {
        NoOpLlmGateway().stream("test").test {
            awaitComplete()
        }
    }

    @Test
    fun `LlmAssistantEngine with NoOp gateway emits Done with empty text`() = runTest {
        val engine = LlmAssistantEngine(NoOpLlmGateway())
        var doneText: String? = null
        engine.respond("conv-1", "hi", 0).test {
            while (true) {
                val item = awaitItem()
                if (item is AssistantChunk.Done) { doneText = item.fullText; break }
            }
            awaitComplete()
        }
        assertEquals("", doneText)
    }

    @Test
    fun `title suggestion set on first message`() = runTest {
        val engine = LlmAssistantEngine(NoOpLlmGateway())
        var title: String? = null
        engine.respond("conv-1", "What is the rate?", 0).test {
            while (true) {
                val item = awaitItem()
                if (item is AssistantChunk.Done) { title = item.titleSuggestion; break }
            }
            awaitComplete()
        }
        assertTrue(title != null)
    }
}
