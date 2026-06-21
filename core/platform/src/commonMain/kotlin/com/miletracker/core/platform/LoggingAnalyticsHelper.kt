package com.miletracker.core.platform

import io.github.aakira.napier.Napier

/**
 * CF.2: analytics sink that logs (self-clamped) events via Napier. This is the noGms + iOS analytics impl
 * (no real backend); the gms flavor binds a Firebase-backed helper (CF.3).
 */
class LoggingAnalyticsHelper : AnalyticsHelper {
    override fun log(event: AnalyticsEvent) {
        Napier.d(tag = TAG) { "${event.safeType} ${event.safeParams}" }
    }

    override fun setUserProperty(
        name: String,
        value: String?,
    ) {
        Napier.d(tag = TAG) { "userProperty ${name.take(AnalyticsEvent.MAX_NAME)}=$value" }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
