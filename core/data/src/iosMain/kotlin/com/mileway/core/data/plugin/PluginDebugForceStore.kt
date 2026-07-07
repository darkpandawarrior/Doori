package com.mileway.core.data.plugin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

private const val PREFIX = "force_"

/** iOS mirror of the debug-force store (see the androidMain doc). */
class PluginDebugForceStore : PluginDebugForceSource {
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "plugin_debug_forces.preferences_pb").toPath() },
        )

    override val overrides: Flow<Map<String, String>> =
        store.data.map { prefs ->
            prefs.asMap()
                .filterKeys { it.name.startsWith(PREFIX) }
                .entries
                .associate { (key, value) -> key.name.removePrefix(PREFIX) to value.toString() }
        }

    override suspend fun setForce(
        id: String,
        raw: String?,
    ) {
        store.edit { prefs ->
            val key = stringPreferencesKey(PREFIX + id)
            if (raw == null) prefs.remove(key) else prefs[key] = raw
        }
    }

    override suspend fun clearAll() {
        store.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { prefs.remove(it) }
        }
    }
}
