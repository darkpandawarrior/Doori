package com.mileway.feature.agent.di

import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.engine.OfflineAssistantEngine
import com.mileway.feature.agent.engine.llm.LlmAssistantEngine
import com.mileway.feature.agent.engine.llm.LlmGateway
import com.mileway.feature.agent.repository.AgentRepository
import com.mileway.feature.agent.viewmodel.AgentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val agentPlatformModule: Module

/**
 * Picks the real on-device [LlmAssistantEngine] when [gateway] reports capable hardware (ML Kit
 * GenAI on Android, Foundation Models on iOS — both EXPERIMENTAL, see their gateway impls), else
 * falls back to the deterministic [OfflineAssistantEngine] — the same degrade path this app always
 * had. A plain function (not inlined into the Koin DSL) so it's unit-testable without a Koin graph.
 */
internal fun selectAssistantEngine(
    gateway: LlmGateway,
    offline: () -> AssistantEngine,
): AssistantEngine = if (gateway.isAvailable()) LlmAssistantEngine(gateway) else offline()

val agentModule =
    module {
        includes(agentPlatformModule)
        single { AgentRepository(get(), get()) }
        single<AssistantEngine> { selectAssistantEngine(get()) { OfflineAssistantEngine(get()) } }
        viewModelOf(::AgentViewModel)
    }
