package com.mileway.feature.agent.engine

import com.mileway.feature.agent.engine.llm.LlmAssistantEngine
import com.mileway.feature.agent.engine.llm.LlmGateway
import com.siddharth.kmp.ai.testing.FakeOnDeviceLlm
import com.siddharth.kmp.result.AiFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

// Mirrors MlKitLlmGateway/FoundationModelsLlmGateway's shape exactly — both are just this same
// isAvailable()/generateStream() delegation, per-platform. A FakeOnDeviceLlm-backed one, here,
// lets LlmAssistantEngine be exercised in commonTest with no real model or device.
private class FakeLlmGateway(private val llm: FakeOnDeviceLlm) : LlmGateway {
    override fun isAvailable(): Boolean = llm.isAvailable()

    override fun stream(prompt: String): Flow<String> = llm.generateStream(prompt)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LlmAssistantEngineTest {
    @Test
    fun `streams tokens then Done with the concatenated reply and a title on a new thread`() =
        runTest {
            val llm = FakeOnDeviceLlm()
            llm.enqueueStreamChunks("Hello", " ", "world")
            val engine = LlmAssistantEngine(FakeLlmGateway(llm))

            val chunks = engine.respond("thread-1", "hi", historySize = 0).toList()

            assertIs<AssistantChunk.Thinking>(chunks.first())
            val tokens = chunks.drop(1).dropLast(1).map { assertIs<AssistantChunk.Token>(it).text }
            assertEquals(listOf("Hello", " ", "world"), tokens)
            val done = assertIs<AssistantChunk.Done>(chunks.last())
            assertEquals("Hello world", done.fullText)
            assertNotNull(done.titleSuggestion)
        }

    @Test
    fun `no title suggestion once the thread already has history`() =
        runTest {
            val llm = FakeOnDeviceLlm()
            llm.enqueueStreamChunks("ok")
            val engine = LlmAssistantEngine(FakeLlmGateway(llm))

            val done = assertIs<AssistantChunk.Done>(engine.respond("thread-1", "hi", historySize = 2).toList().last())

            assertEquals(null, done.titleSuggestion)
        }

    @Test
    fun `a stream that never emits a token ends in EmptyReply, not a blank Done`() =
        runTest {
            // No chunks queued: FakeOnDeviceLlm.generateStream() completes immediately with nothing —
            // the same shape a real on-device backend gives back on a swallowed mid-stream hiccup
            // (MlKitGenAiOnDeviceLlm.generateStream's own .catch {} degrades the same way).
            val engine = LlmAssistantEngine(FakeLlmGateway(FakeOnDeviceLlm()))

            val chunks = engine.respond("thread-1", "hi", historySize = 0).toList()

            val error = assertIs<AssistantChunk.Error>(chunks.last())
            assertEquals(AiFailure.EmptyReply, error.reason)
        }

    @Test
    fun `a mid-stream failure surfaces as an AiFailure error instead of a silently dropped reply`() =
        runTest {
            val throwingGateway =
                object : LlmGateway {
                    override fun isAvailable(): Boolean = true

                    override fun stream(prompt: String): Flow<String> =
                        flow {
                            emit("partial reply")
                            throw IllegalStateException("model crashed mid-stream")
                        }
                }
            val engine = LlmAssistantEngine(throwingGateway)

            val chunks = engine.respond("thread-1", "hi", historySize = 0).toList()

            // The tokens that already arrived are not discarded — only the tail turns into an error.
            assertEquals("partial reply", assertIs<AssistantChunk.Token>(chunks[1]).text)
            val error = assertIs<AssistantChunk.Error>(chunks.last())
            assertEquals(AiFailure.Network, error.reason)
        }

    @Test
    fun `cancelling the Stop way propagates cancellation, never rewrites it into an Error chunk`() =
        runTest {
            val neverFinishes =
                object : LlmGateway {
                    override fun isAvailable(): Boolean = true

                    override fun stream(prompt: String): Flow<String> =
                        flow {
                            emit("a")
                            delay(10_000)
                            emit("b")
                        }
                }
            val engine = LlmAssistantEngine(neverFinishes)
            val collected = mutableListOf<AssistantChunk>()

            val job = launch { engine.respond("thread-1", "hi", historySize = 0).collect { collected += it } }
            advanceTimeBy(1)
            job.cancelAndJoin()

            assertFalse(collected.any { it is AssistantChunk.Error })
        }
}
