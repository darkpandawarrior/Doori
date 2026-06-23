package com.miletracker.feature.agent.di

import com.miletracker.feature.agent.analytics.AgentAnalyticsStore
import com.miletracker.feature.agent.analytics.DataStoreAgentAnalyticsStore
import com.miletracker.feature.agent.voice.AndroidSpeechToText
import com.miletracker.feature.agent.voice.AndroidTextToSpeech
import com.miletracker.feature.agent.voice.SpeechToText
import com.miletracker.feature.agent.voice.TextToSpeech
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val agentPlatformModule: Module =
    module {
        single<SpeechToText> { AndroidSpeechToText(androidContext()) }
        single<TextToSpeech> { AndroidTextToSpeech(androidContext()) }
        single<AgentAnalyticsStore> { DataStoreAgentAnalyticsStore(androidContext()) }
    }
