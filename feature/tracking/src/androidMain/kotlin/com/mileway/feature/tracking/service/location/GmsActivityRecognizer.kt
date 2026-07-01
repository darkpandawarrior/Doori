package com.mileway.feature.tracking.service.location

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * O.2: Play Services [ActivityRecognition]-backed [ActivityRecognizer]. While collected, it registers a
 * receiver for periodic activity updates and emits the most-probable activity (mapped via
 * [ActivityTypeMapper]). Requesting updates and (un)registering are wrapped in runCatching, so on a device
 * without Play Services it just never emits — the IMU MotionState fusion (O.1/O.3) remains the offline
 * stillness source. Requires the ACTIVITY_RECOGNITION runtime permission to actually deliver.
 */
class GmsActivityRecognizer(private val context: Context) : ActivityRecognizer {
    override val activity: Flow<RecognizedActivity> =
        callbackFlow {
            val client = ActivityRecognition.getClient(context)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        ctx: Context?,
                        intent: Intent?,
                    ) {
                        val result = intent?.let { ActivityRecognitionResult.extractResult(it) } ?: return
                        trySend(ActivityTypeMapper.fromDetectedType(result.mostProbableActivity.type))
                    }
                }
            val pending =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(ACTION).setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            runCatching { client.requestActivityUpdates(DETECTION_INTERVAL_MS, pending) }
            awaitClose {
                runCatching { client.removeActivityUpdates(pending) }
                runCatching { context.unregisterReceiver(receiver) }
            }
        }.distinctUntilChanged()

    private companion object {
        const val ACTION = "com.mileway.ACTIVITY_RECOGNITION"
        const val DETECTION_INTERVAL_MS = 10_000L
    }
}
