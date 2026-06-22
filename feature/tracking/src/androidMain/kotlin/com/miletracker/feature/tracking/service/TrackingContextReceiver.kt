package com.miletracker.feature.tracking.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.PowerManager
import io.github.aakira.napier.Napier

class TrackingContextReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TrackingContextReceiver"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val enabled = pm.isPowerSaveMode
                Napier.i("Power saver: $enabled", tag = TAG)
                broadcastHardwareEvent(context, if (enabled) "Power Saver Mode Enabled" else "Power Saver Mode Disabled")
            }

            ConnectivityManager.CONNECTIVITY_ACTION -> {
                Napier.d("Network connectivity changed", tag = TAG)
            }

            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val event = if (gpsEnabled) "GPS Available" else "GPS Lost"
                Napier.i(event, tag = TAG)
                broadcastHardwareEvent(context, event)
            }

            Intent.ACTION_BATTERY_LOW -> {
                Napier.w("Battery low", tag = TAG)
                broadcastHardwareEvent(context, "Battery Low")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0) {
                    val pct = (level * 100f / scale).toInt()
                    if (pct % 10 == 0) Napier.d("Battery: $pct%", tag = TAG)
                }
            }

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val enabled = intent.getBooleanExtra("state", false)
                Napier.i("Airplane mode: $enabled", tag = TAG)
                if (enabled) broadcastHardwareEvent(context, "Airplane Mode Enabled")
            }

            // P-C.3: L4 — device is shutting down. The service may already be dead so we write
            // a durable SharedPreferences flag instead of broadcasting to it. The service
            // consumes and clears this flag once in restoreSession() after the next boot.
            Intent.ACTION_SHUTDOWN -> {
                Napier.w("Device shutdown — setting shutdown flag", tag = TAG)
                val prefs =
                    context.getSharedPreferences(
                        AndroidShutdownFlagStore.PREFS_NAME,
                        Context.MODE_PRIVATE,
                    )
                AndroidShutdownFlagStore(prefs).set()
            }
        }
    }

    private fun broadcastHardwareEvent(
        context: Context,
        eventText: String,
    ) {
        val broadcastIntent =
            Intent(LocationTrackingConstants.ACTION_TRACKING_STARTED).apply {
                setPackage(context.packageName)
                putExtra("hw_event_text", eventText)
                putExtra("hw_event_time", System.currentTimeMillis())
            }
        context.sendBroadcast(broadcastIntent)
    }
}
