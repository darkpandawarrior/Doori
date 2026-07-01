package com.mileway.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.agentSessionDataStore by preferencesDataStore(name = "agent_session")

class AgentSessionStoreImpl(private val context: Context) : AgentSessionStore {
    private val threadIdKey = stringPreferencesKey("agent_active_thread_id")
    private val lastActiveKey = longPreferencesKey("agent_last_active_ms")

    override suspend fun getActiveThread(): Pair<String, Long>? =
        context.agentSessionDataStore.data.map { prefs ->
            val id = prefs[threadIdKey] ?: return@map null
            val ms = prefs[lastActiveKey] ?: return@map null
            id to ms
        }.firstOrNull()

    override suspend fun setActiveThread(threadId: String, nowMs: Long) {
        context.agentSessionDataStore.edit { prefs ->
            prefs[threadIdKey] = threadId
            prefs[lastActiveKey] = nowMs
        }
    }

    override suspend fun clearActiveThread() {
        context.agentSessionDataStore.edit { prefs ->
            prefs.remove(threadIdKey)
            prefs.remove(lastActiveKey)
        }
    }
}
