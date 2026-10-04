package com.mileway.feature.logging.justification

import com.mileway.core.ai.assistant.buildCloudFallback
import com.mileway.core.data.domain.claim.JustificationReason
import com.siddharth.kmp.llmchat.ProviderId
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.getOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Optional BYOK hint. Sends only category, amount and policy flag codes as expense context.
 * No merchant, employee id, affidavit note or justification text enters the prompt.
 * No key, failed request, timeout or non-exact wire code yields no hint, with no retry or logging.
 * A returned code is a hint only; the caller must wait for an explicit user selection.
 */
class JustificationReasonSuggester(
    private val getKey: (ProviderId) -> String?,
    private val generate: suspend (String) -> AiResult<String> = { buildCloudFallback(getKey).generate(it) },
) {
    suspend fun suggest(
        category: String,
        amountMinor: Long,
        flagCodes: List<String>,
    ): JustificationReason? {
        return try {
            if (CloudProviders.none { !getKey(it).isNullOrBlank() }) return null
            val answer =
                withTimeoutOrNull(SuggestionTimeoutMillis) {
                    generate(
                        "Suggest one expense exception code: ${JustificationReason.entries.joinToString { it.wireName }}. " +
                            "Return only the exact code. Category=$category; amountMinor=$amountMinor; " +
                            "policyFlags=${flagCodes.joinToString()}.",
                    ).getOrNull()
                }
            JustificationReason.entries.find { it.wireName == answer }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

private val CloudProviders = listOf(ProviderId.ANTHROPIC, ProviderId.OPENAI, ProviderId.GEMINI)
private const val SuggestionTimeoutMillis = 8_000L
