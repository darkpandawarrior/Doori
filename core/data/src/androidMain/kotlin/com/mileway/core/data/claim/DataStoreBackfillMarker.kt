package com.mileway.core.data.claim

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.legacyBackfillDataStore by preferencesDataStore(name = "legacy_mileage_backfill")

// Keyed to the schema version the backfill's target tables landed in (MIGRATION_48_49) — bumping
// this key (not just its value) is how a later lane would force every device to re-run the pass
// (e.g. after fixing a backfill bug), without needing a new Room migration.
private val SCHEMA_VERSION_MARKER_KEY = booleanPreferencesKey("schemaVersionMarker_49")

/** DataStore-backed [BackfillMarker] — mirrors [com.mileway.core.data.plugin.PluginDebugForceStore]'s pattern. */
class DataStoreBackfillMarker(
    private val context: Context,
) : BackfillMarker {
    override suspend fun isDone(): Boolean = context.legacyBackfillDataStore.data.first()[SCHEMA_VERSION_MARKER_KEY] ?: false

    override suspend fun markDone() {
        context.legacyBackfillDataStore.edit { it[SCHEMA_VERSION_MARKER_KEY] = true }
    }
}
