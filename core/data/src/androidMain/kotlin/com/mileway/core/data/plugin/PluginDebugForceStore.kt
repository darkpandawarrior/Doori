package com.mileway.core.data.plugin

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pluginForceDataStore by preferencesDataStore(name = "plugin_debug_forces")
private const val PREFIX = "force_"

/**
 * PLAN_V24 P0.1 — DataStore-backed [PluginDebugForceSource] (the FORCED layer). Not per-account —
 * forces survive account switch, mirroring the reference app's `DebugDataStore`. Each force is one string pref
 * keyed `force_<pluginId>`.
 */
class PluginDebugForceStore(private val context: Context) : PluginDebugForceSource {
    override val overrides: Flow<Map<String, String>> =
        context.pluginForceDataStore.data.map { prefs ->
            prefs.asMap()
                .filterKeys { it.name.startsWith(PREFIX) }
                .entries
                .associate { (key, value) -> key.name.removePrefix(PREFIX) to value.toString() }
        }

    override suspend fun setForce(
        id: String,
        raw: String?,
    ) {
        context.pluginForceDataStore.edit { prefs ->
            val key = stringPreferencesKey(PREFIX + id)
            if (raw == null) prefs.remove(key) else prefs[key] = raw
        }
    }

    override suspend fun clearAll() {
        context.pluginForceDataStore.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { prefs.remove(it) }
        }
    }
}
