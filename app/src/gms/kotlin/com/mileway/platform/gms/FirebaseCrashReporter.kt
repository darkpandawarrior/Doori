package com.mileway.platform.gms

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.siddharth.kmp.common.CrashReporter

/**
 * CF.4: gms crash reporter backed by Firebase Crashlytics. Collection is disabled in the manifest by
 * default (privacy); [setEnabled] flips it at runtime once the user consents. Without a real Firebase
 * config the SDK simply queues nothing, never crashes.
 */
class FirebaseCrashReporter : CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(
        throwable: Throwable,
        message: String?,
    ) {
        message?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(id: String?) {
        crashlytics.setUserId(id ?: "")
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }
}
