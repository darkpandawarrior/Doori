package com.miletracker.feature.agent.di

import com.miletracker.feature.agent.engine.AssistantEngine
import com.miletracker.feature.agent.engine.OfflineAssistantEngine
import com.miletracker.feature.agent.repository.AgentRepository
import com.miletracker.feature.agent.viewmodel.AgentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val agentPlatformModule: Module

val agentModule =
    module {
        includes(agentPlatformModule)
        single { AgentRepository(get(), get()) }
        single<AssistantEngine> { OfflineAssistantEngine(get()) }
        viewModelOf(::AgentViewModel)
    }
