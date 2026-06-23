package com.miletracker.feature.agent

import com.miletracker.feature.agent.di.agentModule

/**
 * Exposes agentModule for use in the iOS Koin bootstrap.
 *
 * Swift callers should include this in their initKoin call alongside trackingModule.
 * Lives in feature:agent/iosMain so it can see both coreUiModule and agentModule without
 * creating a cycle (feature:agent → core:ui, not the reverse).
 */
val iosAgentModule get() = agentModule
