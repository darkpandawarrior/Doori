package com.mileway

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.ActivityTypeMapper
import com.mileway.feature.tracking.service.location.RecognizedActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Active-recording stillness stream, backed by GMS transitions rather than periodic polling. */
class GmsActivityRecognizer(
    private val context: Context,
) : ActivityRecognizer {
    override val activity: Flow<RecognizedActivity> =
        callbackFlow {
            val client = ActivityRecognition.getClient(context)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        ctx: Context?,
                        intent: Intent?,
                    ) {
                        val result = intent?.let { ActivityTransitionResult.extractResult(it) } ?: return
                        for (event in result.transitionEvents) {
                            trySend(
                                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                    ActivityTypeMapper.fromDetectedType(event.activityType)
                                } else {
                                    RecognizedActivity.UNKNOWN
                                },
                            )
                        }
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
            val transitions =
                listOf(DetectedActivity.STILL, DetectedActivity.IN_VEHICLE, DetectedActivity.ON_FOOT, DetectedActivity.ON_BICYCLE)
                    .flatMap { activity ->
                        listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT).map { transition ->
                            ActivityTransition
                                .Builder()
                                .setActivityType(activity)
                                .setActivityTransition(transition)
                                .build()
                        }
                    }
            runCatching { client.requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pending) }
            awaitClose {
                runCatching { client.removeActivityTransitionUpdates(pending) }
                runCatching { context.unregisterReceiver(receiver) }
            }
        }.distinctUntilChanged()

    private companion object {
        const val ACTION = "com.mileway.ACTIVITY_RECOGNITION"
    }
}
