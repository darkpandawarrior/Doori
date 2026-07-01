package com.mileway.core.data.settings

interface AgentSessionStore {
    suspend fun getActiveThread(): Pair<String, Long>?
    suspend fun setActiveThread(threadId: String, nowMs: Long)
    suspend fun clearActiveThread()
}
