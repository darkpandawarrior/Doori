package com.mileway.feature.agent.di

import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.analytics.DataStoreAgentAnalyticsStore
import com.mileway.feature.agent.engine.llm.LlmGateway
import com.mileway.feature.agent.engine.llm.MlKitLlmGateway
import com.mileway.feature.agent.voice.AndroidSpeechToText
import com.mileway.feature.agent.voice.AndroidTextToSpeech
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.voice.TextToSpeech
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val agentPlatformModule: Module =
    module {
        single<SpeechToText> { AndroidSpeechToText(androidContext()) }
        single<TextToSpeech> { AndroidTextToSpeech(androidContext()) }
        single<AgentAnalyticsStore> { DataStoreAgentAnalyticsStore(androidContext()) }
        // EXPERIMENTAL — ML Kit GenAI Prompt API (Gemini Nano), see MlKitLlmGateway.
        single<LlmGateway> { MlKitLlmGateway(androidContext()) }
    }
