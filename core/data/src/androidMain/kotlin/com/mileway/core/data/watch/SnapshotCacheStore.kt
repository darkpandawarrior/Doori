package com.mileway.core.data.watch

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.snapshotCacheDataStore by preferencesDataStore(name = "snapshot_cache")

/**
 * Android actual for [SnapshotCache] (P6.1). A home-screen widget's `GlanceAppWidgetReceiver`
 * runs in the SAME app process as the rest of `:app` (Android app widgets are not a separate
 * process/app the way an iOS extension is), so a plain app-private DataStore file — the exact
 * idiom [com.mileway.core.data.session.ActiveAccountStore] already uses — is a widget-readable
 * location: any component holding this same `Context` (the widget's `provideGlance`, a future
 * `GlanceAppWidgetReceiver.onUpdate`) can construct the same [SnapshotCacheStore] and read it.
 */
class SnapshotCacheStore(
    private val context: Context,
) : SnapshotCache {
    private val payloadKey = stringPreferencesKey("watch_sync_payload_json")

    override suspend fun write(payload: WatchSyncPayload) {
        context.snapshotCacheDataStore.edit { prefs ->
            prefs[payloadKey] = SnapshotCacheCodec.encode(payload)
        }
    }

    override suspend fun read(): WatchSyncPayload? {
        // Single-shot read: androidx.datastore.core.DataStore.data is a Flow, but this cache
        // contract is a plain suspend getter (mirrors ActiveAccountStore/SessionRepository's
        // suspend-setter shape) — first() is the standard single-read idiom here.
        val prefs = context.snapshotCacheDataStore.data.first()
        return SnapshotCacheCodec.decode(prefs[payloadKey])
    }
}
