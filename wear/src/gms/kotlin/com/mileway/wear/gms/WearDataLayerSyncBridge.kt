package com.mileway.wear.gms

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mileway.core.data.watch.SnapshotCacheCodec
import com.mileway.core.data.watch.WatchSyncBridge
import com.mileway.core.data.watch.WatchSyncPayload
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await

private const val TAG = "WearDataLayerSyncBridge"

/** DataLayer path both the phone (`PhoneSnapshotPublisher`, P2.9) and the watch write/read on. */
internal const val SNAPSHOT_SYNC_PATH = "/mileway/snapshot"
private const val PAYLOAD_JSON_KEY = "payload_json"

/**
 * P2.9: the `wear/src/gms` [WatchSyncBridge] implementation, over the Wear OS Data Layer
 * (`Wearable.getDataClient`). Confined to `wear/src/gms` per the flavor-isolation gotcha in
 * PLAN_V23 §7 — `noGms`/`main` code only ever sees the [WatchSyncBridge] interface (P1.3), never
 * this class or any `com.google.android.gms.wearable.*` type.
 *
 * Reuses [SnapshotCacheCodec] (P6.1) for the JSON encode/decode of [WatchSyncPayload] so the wire
 * shape is identical to the one the widget-facing [com.mileway.core.data.watch.SnapshotCache]
 * already round-trips and unit-tests — this bridge adds only the DataLayer transport around that
 * already-tested codec, not a second one.
 */
class WearDataLayerSyncBridge(
    context: Context,
) : WatchSyncBridge {
    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext)

    override suspend fun push(payload: WatchSyncPayload) {
        val request =
            PutDataMapRequest.create(SNAPSHOT_SYNC_PATH).apply {
                dataMap.putString(PAYLOAD_JSON_KEY, SnapshotCacheCodec.encode(payload))
            }
        runCatching {
            dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
        }.onFailure { e ->
            Napier.e(tag = TAG, message = "push failed", throwable = e)
        }
    }

    override suspend fun latest(): WatchSyncPayload? =
        runCatching {
            dataClient.dataItems.await().use { buffer ->
                buffer
                    .asSequence()
                    .firstOrNull { it.uri.path == SNAPSHOT_SYNC_PATH }
                    ?.let { it.decodePayload() }
            }
        }.onFailure { e ->
            Napier.e(tag = TAG, message = "latest failed", throwable = e)
        }.getOrNull()

    override fun observeIncoming(): Flow<WatchSyncPayload> =
        callbackFlow {
            val listener =
                DataClient.OnDataChangedListener { events: DataEventBuffer ->
                    events.forEach { event ->
                        if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == SNAPSHOT_SYNC_PATH) {
                            event.dataItem.decodePayload()?.let { trySend(it) }
                        }
                    }
                    events.release()
                }
            dataClient.addListener(listener)
            awaitClose { dataClient.removeListener(listener) }
        }.mapNotNull { it }

    private fun DataItem.decodePayload(): WatchSyncPayload? {
        val json = DataMapItem.fromDataItem(this).dataMap.getString(PAYLOAD_JSON_KEY)
        return SnapshotCacheCodec.decode(json)
    }
}
