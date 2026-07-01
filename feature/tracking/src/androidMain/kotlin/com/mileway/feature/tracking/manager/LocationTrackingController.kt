package com.mileway.feature.tracking.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mileway.feature.tracking.service.LocationTrackingService

/**
 * Android implementation of [TrackingController]: drives [LocationTrackingService] via Intent
 * dispatch without exposing Context to the ViewModel. Held as a Koin singleton.
 */
class LocationTrackingController(private val context: Context) : TrackingController {
    override fun start(token: String) = send(LocationTrackingService.ACTION_START, token, foreground = true)

    override fun pause(token: String) = send(LocationTrackingService.ACTION_PAUSE, token)

    override fun resume(token: String) = send(LocationTrackingService.ACTION_RESUME, token)

    override fun stop(token: String) = send(LocationTrackingService.ACTION_STOP, token)

    private fun send(
        action: String,
        token: String,
        foreground: Boolean = false,
    ) {
        val intent =
            Intent(context, LocationTrackingService::class.java).apply {
                this.action = action
                putExtra(LocationTrackingService.EXTRA_TOKEN, token)
            }
        if (foreground) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
