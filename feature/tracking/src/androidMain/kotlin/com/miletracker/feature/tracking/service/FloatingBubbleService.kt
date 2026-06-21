package com.miletracker.feature.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.miletracker.feature.tracking.TrackMilesActivity

class FloatingBubbleService : Service() {
    companion object {
        private const val TAG = "FloatingBubbleService"
        private const val CHANNEL_ID = "miletracker_bubble"
        private const val NOTIF_ID = 1002

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted — skipping bubble")
                return
            }
            context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingBubbleService::class.java))
        }
    }

    private lateinit var bubbleManager: FloatingBubbleManager

    private val trackingStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    LocationTrackingConstants.ACTION_TRACKING_STOPPED -> {
                        Log.d(TAG, "Tracking stopped — dismissing bubble")
                        bubbleManager.dismiss { stopSelf() }
                    }
                    LocationTrackingConstants.ACTION_TRACKING_PAUSED ->
                        bubbleManager.updateTrackingState(isTracking = false)
                    LocationTrackingConstants.ACTION_TRACKING_RESUMED ->
                        bubbleManager.updateTrackingState(isTracking = true)
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        bubbleManager = FloatingBubbleManager(this)
        registerTrackingReceiver()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (!enterForeground()) return START_NOT_STICKY

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission missing — stopping bubble service")
            stopSelf()
            return START_NOT_STICKY
        }

        bubbleManager.showBubble(
            isTracking = true,
            savedX = 16,
            savedY = 300,
            onTap = { openTrackingActivity() },
        )

        return START_STICKY
    }

    /** Calls startForeground defensively. Returns false (and stops self) on failure. */
    private fun enterForeground(): Boolean =
        try {
            startForeground(
                NOTIF_ID,
                buildSilentNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed", e)
            stopSelf()
            false
        }

    override fun onDestroy() {
        bubbleManager.forceRemove()
        try {
            unregisterReceiver(trackingStateReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun openTrackingActivity() {
        bubbleManager.vibrate()
        startActivity(
            Intent(this, TrackMilesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
    }

    private fun registerTrackingReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(LocationTrackingConstants.ACTION_TRACKING_STOPPED)
                addAction(LocationTrackingConstants.ACTION_TRACKING_PAUSED)
                addAction(LocationTrackingConstants.ACTION_TRACKING_RESUMED)
            }
        registerReceiver(trackingStateReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun buildSilentNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Floating Bubble", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) },
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Mileway")
            .setContentText("Live tracking bubble active")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }
}
