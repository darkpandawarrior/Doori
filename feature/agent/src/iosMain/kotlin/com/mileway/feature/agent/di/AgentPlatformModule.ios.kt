package com.mileway.feature.agent.di

import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.analytics.IosAgentAnalyticsStore
import com.mileway.feature.agent.engine.llm.FoundationModelsLlmGateway
import com.mileway.feature.agent.engine.llm.LlmGateway
import com.mileway.feature.agent.voice.IosSpeechToText
import com.mileway.feature.agent.voice.IosTextToSpeech
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.voice.TextToSpeech
import org.koin.core.module.Module
import org.koin.dsl.module

actual val agentPlatformModule: Module =
    module {
        single<SpeechToText> { IosSpeechToText() }
        single<TextToSpeech> { IosTextToSpeech() }
        single<AgentAnalyticsStore> { IosAgentAnalyticsStore() }
        // EXPERIMENTAL — Apple Foundation Models, see FoundationModelsLlmGateway.
        single<LlmGateway> { FoundationModelsLlmGateway() }
    }
