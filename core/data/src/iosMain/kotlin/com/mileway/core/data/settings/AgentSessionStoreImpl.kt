package com.mileway.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

class AgentSessionStoreImpl : AgentSessionStore {
    private val threadIdKey = stringPreferencesKey("agent_active_thread_id")
    private val lastActiveKey = longPreferencesKey("agent_last_active_ms")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "agent_session.preferences_pb").toPath() },
        )

    override suspend fun getActiveThread(): Pair<String, Long>? =
        store.data.map { prefs ->
            val id = prefs[threadIdKey] ?: return@map null
            val ms = prefs[lastActiveKey] ?: return@map null
            id to ms
        }.firstOrNull()

    override suspend fun setActiveThread(
        threadId: String,
        nowMs: Long,
    ) {
        store.edit { prefs ->
            prefs[threadIdKey] = threadId
            prefs[lastActiveKey] = nowMs
        }
    }

    override suspend fun clearActiveThread() {
        store.edit { prefs ->
            prefs.remove(threadIdKey)
            prefs.remove(lastActiveKey)
        }
    }
}
