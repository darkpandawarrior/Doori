package com.mileway.core.ai.assistant

import com.siddharth.kmp.ai.CloudOnDeviceLlm
import com.siddharth.kmp.llmchat.AiConfig
import com.siddharth.kmp.llmchat.AiMessage
import com.siddharth.kmp.llmchat.AiProvider
import com.siddharth.kmp.llmchat.ProviderId
import com.siddharth.kmp.llmchat.buildProviderChain
import com.siddharth.kmp.llmchat.loadAiProviderConfig
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.AiResult
import com.siddharth.kmp.result.Result

/**
 * Chain end for [buildCloudFallback]: this app wires no app-specific offline heuristic into the
 * cloud chain here (unlike feature:agent's OfflineAssistantEngine, a separate seam — see the lane
 * brief). Nothing configured reads as [AiFailure.NoKey], same bucket a genuinely-unset BYOK key
 * lands in.
 */
private object NoCloudProviderConfigured : AiProvider {
    override val id = "none"
    override val displayName = "No provider configured"

    override suspend fun complete(
        messages: List<AiMessage>,
        config: AiConfig,
    ): AiResult<String> = Result.Failure(AiFailure.NoKey)

    override suspend fun isAvailable(): Boolean = false
}

/**
 * The cloud-fallback [com.siddharth.kmp.ai.OnDeviceLlm] tier: BYOK Anthropic/OpenAI/Gemini
 * (whichever keys [getKey] returns), tried [selectedProvider]-first and guarded by
 * [com.siddharth.kmp.result.PromptGuard] (see [buildProviderChain]). This is the seam desktop (no
 * on-device model at all) falls straight to, and the one any phone without Nano/Foundation-Models
 * hardware would fall through to as well.
 *
 * [getKey] takes a plain function rather than a [com.siddharth.kmp.llmchat.SecureKeyStore] directly
 * — that type has no common-code constructor (Android needs a `Context`), so it can only be built
 * per platform; pass `keyStore::getKey` at the call site, same idiom
 * [com.siddharth.kmp.llmchat.loadAiProviderConfig] and [com.siddharth.kmp.designsystem.ai.AiSettingsState]
 * already use.
 */
fun buildCloudFallback(
    getKey: (ProviderId) -> String?,
    selectedProvider: ProviderId = ProviderId.OFFLINE_FALLBACK,
): CloudOnDeviceLlm {
    val config = loadAiProviderConfig(getKey, selectedProvider)
    return CloudOnDeviceLlm(buildProviderChain(config, fallback = NoCloudProviderConfigured))
}
