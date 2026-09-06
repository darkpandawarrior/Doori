package com.mileway.feature.agent.engine.llm

import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.engine.ConversationTitler
import com.siddharth.kmp.result.AiFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LlmAssistantEngine(private val gateway: LlmGateway) : AssistantEngine {
    // SwallowedException: AiFailure (design-frozen, see AGENTS.md) has no free-text/cause slot to
    // carry `failure` into — the catch below already explains why Network is the closest reason.
    @Suppress("SwallowedException")
    override fun respond(
        conversationId: String,
        userMessage: String,
        historySize: Int,
    ): Flow<AssistantChunk> =
        flow {
            emit(AssistantChunk.Thinking("Thinking…"))
            var fullText = ""
            try {
                gateway.stream(userMessage).collect { token ->
                    fullText += token
                    emit(AssistantChunk.Token(token))
                }
            } catch (cancellation: CancellationException) {
                // Stop button (or navigating away) cancels the collecting coroutine — let that
                // propagate as a real cancellation, never rewrite it into an Error chunk.
                throw cancellation
            } catch (failure: Exception) {
                // gateway.stream() itself isn't documented to throw (every real backend already
                // catches its own mid-stream hiccups — see MlKitGenAiOnDeviceLlm.generateStream),
                // but a silently-dropped reply on any future backend that does throw is exactly the
                // bug this wrapper exists to close. AiFailure.Network is the closest reason since
                // AiFailure has no generic/unknown case.
                emit(AssistantChunk.Error(AiFailure.Network))
                return@flow
            }
            if (fullText.isEmpty()) {
                // A stream that completed normally but never emitted a token — the on-device
                // backend degraded silently (model hiccup, or generateStream's own default/catch
                // swallowing a failure). Surface it the same way OnDeviceLlm.generate() would.
                emit(AssistantChunk.Error(AiFailure.EmptyReply))
                return@flow
            }
            val title = if (historySize == 0) ConversationTitler.title(userMessage) else null
            emit(AssistantChunk.Done(fullText, title))
        }
}
