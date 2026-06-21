package com.miletracker.platform.gms

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.miletracker.core.platform.AnalyticsEvent
import com.miletracker.core.platform.AnalyticsHelper

/**
 * CF.3: gms analytics impl backed by Firebase Analytics. Events are already self-clamped by
 * [AnalyticsEvent]; without a real google-services config Firebase silently drops them (never crashes).
 */
class FirebaseAnalyticsHelper(context: Context) : AnalyticsHelper {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun log(event: AnalyticsEvent) {
        analytics.logEvent(event.safeType) {
            event.safeParams.forEach { (key, value) -> param(key, value) }
        }
    }

    override fun setUserProperty(
        name: String,
        value: String?,
    ) {
        analytics.setUserProperty(name.take(AnalyticsEvent.MAX_NAME), value)
    }
}
