package com.mileway.feature.agent.analytics

interface AgentAnalyticsStore {
    suspend fun recordQuestion(intent: String)
    suspend fun getHitCounts(): Map<String, Int>
}
