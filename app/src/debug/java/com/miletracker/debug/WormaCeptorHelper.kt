package com.miletracker.debug

import android.content.Context
import android.content.Intent
import com.azikar24.wormaceptor.api.Feature
import com.azikar24.wormaceptor.api.WormaCeptorApi

/**
 * Debug implementation, provides live HTTP inspection via WormaCeptor.
 *
 * Initialise once from Application.onCreate().
 *
 * Use [getLaunchIntent] to open the WormaCeptor traffic viewer from the Debug Menu.
 *
 * Note: this app is fully offline (no OkHttp clients), so no request interceptor is wired.
 */
object WormaCeptorHelper {

    fun init(context: Context) {
        WormaCeptorApi.init(
            context = context,
            logCrashes = true,
            leakNotifications = true,
            features = Feature.ALL,
        )
    }

    fun getLaunchIntent(context: Context): Intent? =
        WormaCeptorApi.getLaunchIntent(context)
}
