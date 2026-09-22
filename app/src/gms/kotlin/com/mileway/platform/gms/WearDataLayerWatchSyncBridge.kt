package com.mileway.platform.gms

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

private const val TAG = "WearDataLayerWatchSyncBridge"

/** Same Data Layer path [com.mileway.wear.gms.WearDataLayerSyncBridge] (`:wear`) reads/writes. */
internal const val SNAPSHOT_SYNC_PATH = "/mileway/snapshot"
private const val PAYLOAD_JSON_KEY = "payload_json"

/**
 * P2.9: the phone-side (`app/src/gms`) [WatchSyncBridge] implementation, mirroring
 * `wear/src/gms`'s `WearDataLayerSyncBridge` — same Data Layer path, same
 * [SnapshotCacheCodec] wire shape, opposite peer. [com.mileway.core.data.watch.PhoneSnapshotSync]
 * pushes through this bridge rather than talking to `Wearable.getDataClient` directly, so both
 * ends of the sync go through the identical, already-interface-tested [WatchSyncBridge] contract
 * (P1.3).
 *
 * gms flavor ONLY — confined to `app/src/gms` per the flavor-isolation gotcha in PLAN_V23 §7.
 */
class WearDataLayerWatchSyncBridge(
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
