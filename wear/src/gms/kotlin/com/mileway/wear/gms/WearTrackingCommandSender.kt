package com.mileway.wear.gms

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.mileway.core.data.watch.TrackingCommand
import com.mileway.core.data.watch.TrackingCommandSender
import com.mileway.core.data.watch.TrackingCommandCodec
import io.github.aakira.napier.Napier
import kotlinx.coroutines.tasks.await

private const val TAG = "WearTrackingCommandSender"

/** DataLayer message path both this sender and the phone's `WearTrackingCommandService` (P2.10) use. */
internal const val TRACK_COMMAND_PATH = "/mileway/track-command"

/**
 * The capability the phone side advertises (`app/src/gms/res/values/wear.xml`) so this sender can
 * resolve the paired phone's node id without hard-coding it — mirrors how a real Wear OS
 * companion-command sender discovers its counterpart node.
 */
internal const val PHONE_TRACK_CAPABILITY = "mileway_phone_track"

/**
 * P2.10: the watch's half of the watch->phone start/stop-tracking command (P2.9 is the reverse,
 * phone->watch snapshot direction). Confined to `wear/src/gms` per the flavor-isolation gotcha in
 * PLAN_V23 §7 — `noGms`/`main` `:wear` code never sees `com.google.android.gms.wearable.*`.
 *
 * Uses [CapabilityClient] to resolve the phone node advertising [PHONE_TRACK_CAPABILITY], then
 * [MessageClient] to send the [TrackingCommandCodec]-encoded bytes on [TRACK_COMMAND_PATH] — the
 * phone's `WearTrackingCommandService` (`app/src/gms`, a `WearableListenerService`) decodes and
 * dispatches to `TrackingController.start`/`stop`.
 */
class WearTrackingCommandSender(context: Context) : TrackingCommandSender {
    private val messageClient: MessageClient = Wearable.getMessageClient(context.applicationContext)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context.applicationContext)

    /** Sends a start command for [token] to the paired phone, if one is reachable. */
    override suspend fun sendStart(token: String) = send(TrackingCommand(TrackingCommand.Action.START, token))

    /** Sends a stop command for [token] to the paired phone, if one is reachable. */
    override suspend fun sendStop(token: String) = send(TrackingCommand(TrackingCommand.Action.STOP, token))

    private suspend fun send(command: TrackingCommand) {
        runCatching {
            val nodeId = resolvePhoneNodeId() ?: return
            val bytes = TrackingCommandCodec.encode(command)
            messageClient.sendMessage(nodeId, TRACK_COMMAND_PATH, bytes).await()
        }.onFailure { e ->
            Napier.e(tag = TAG, message = "send failed", throwable = e)
        }
    }

    private suspend fun resolvePhoneNodeId(): String? =
        capabilityClient
            .getCapability(PHONE_TRACK_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
            .firstOrNull()
            ?.id
}
