package com.miletracker

import com.miletracker.core.data.settings.AgentSessionStore

class FakeAgentSessionStore : AgentSessionStore {
    private var stored: Pair<String, Long>? = null

    override suspend fun getActiveThread(): Pair<String, Long>? = stored

    override suspend fun setActiveThread(threadId: String, nowMs: Long) {
        stored = threadId to nowMs
    }

    override suspend fun clearActiveThread() {
        stored = null
    }
}
