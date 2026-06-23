package com.miletracker.feature.agent.di

import com.miletracker.feature.agent.analytics.AgentAnalyticsStore
import com.miletracker.feature.agent.analytics.IosAgentAnalyticsStore
import com.miletracker.feature.agent.voice.IosSpeechToText
import com.miletracker.feature.agent.voice.IosTextToSpeech
import com.miletracker.feature.agent.voice.SpeechToText
import com.miletracker.feature.agent.voice.TextToSpeech
import org.koin.core.module.Module
import org.koin.dsl.module

actual val agentPlatformModule: Module =
    module {
        single<SpeechToText> { IosSpeechToText() }
        single<TextToSpeech> { IosTextToSpeech() }
        single<AgentAnalyticsStore> { IosAgentAnalyticsStore() }
    }
