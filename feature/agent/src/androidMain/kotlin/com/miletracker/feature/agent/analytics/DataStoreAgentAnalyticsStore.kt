package com.miletracker.feature.agent.analytics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.agentAnalyticsDs by preferencesDataStore("agent_analytics")
private val KEY_HITS = stringPreferencesKey("intent_hits")

class DataStoreAgentAnalyticsStore(private val context: Context) : AgentAnalyticsStore {

    override suspend fun recordQuestion(intent: String) {
        context.agentAnalyticsDs.edit { prefs ->
            val current = prefs[KEY_HITS].parseHits()
            val updated = current.toMutableMap().apply { this[intent] = (this[intent] ?: 0) + 1 }
            prefs[KEY_HITS] = updated.encodeHits()
        }
    }

    override suspend fun getHitCounts(): Map<String, Int> {
        val prefs = context.agentAnalyticsDs.data.first()
        return prefs[KEY_HITS].parseHits()
    }

    private fun String?.parseHits(): Map<String, Int> {
        if (isNullOrBlank()) return emptyMap()
        return try {
            buildMap {
                split(",").forEach { entry ->
                    val (k, v) = entry.split("=")
                    put(k, v.toInt())
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    private fun Map<String, Int>.encodeHits() = entries.joinToString(",") { "${it.key}=${it.value}" }
}
