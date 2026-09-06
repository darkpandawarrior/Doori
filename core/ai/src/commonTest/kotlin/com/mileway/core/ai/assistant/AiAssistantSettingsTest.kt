package com.mileway.core.ai.assistant

import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AiAssistantSettingsTest {
    @Test
    fun buildCloudFallback_noKeysConfigured_failsWithNoKey() =
        runTest {
            // buildProviderChain always appends the fallback provider, so isAvailable() (providers
            // .isNotEmpty()) is true even with nothing configured — the real gate is generate()'s
            // result: NoCloudProviderConfigured.isAvailable() is false, so it's skipped and the
            // NoKey default (never overwritten) is what comes back. Provider-selection/ordering
            // itself is kmp-toolkit's own buildProviderChain, already covered by its LlmChatSmokeTest.
            val fallback = buildCloudFallback(getKey = { null })

            val result = fallback.generate("hello")
            assertEquals(AiFailure.NoKey, (result as Result.Failure).error)
        }
}
