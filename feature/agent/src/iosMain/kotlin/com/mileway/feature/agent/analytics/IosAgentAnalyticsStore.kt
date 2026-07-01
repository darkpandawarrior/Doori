package com.mileway.feature.agent.analytics

class IosAgentAnalyticsStore : AgentAnalyticsStore {
    private val hits = mutableMapOf<String, Int>()
    override suspend fun recordQuestion(intent: String) { hits[intent] = (hits[intent] ?: 0) + 1 }
    override suspend fun getHitCounts(): Map<String, Int> = hits.toMap()
}
