package com.mileway.feature.agent.di

import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.engine.OfflineAssistantEngine
import com.mileway.feature.agent.repository.AgentRepository
import com.mileway.feature.agent.viewmodel.AgentViewModel
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
