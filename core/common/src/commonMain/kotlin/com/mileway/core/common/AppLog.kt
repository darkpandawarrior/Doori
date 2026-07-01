package com.mileway.core.common

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/**
 * Single entry point for KMP logging. Initialise once per platform at app start so that
 * `commonMain` code can log via [Napier] without touching `android.util.Log` (which is not
 * available in `commonMain`). [DebugAntilog] routes to Logcat on Android and NSLog on iOS.
 *
 * Call from the Android `Application.onCreate()` (debug builds) and from the iOS entry point.
 */
object AppLog {
    private var initialised = false

    fun init() {
        if (initialised) return
        Napier.base(DebugAntilog())
        initialised = true
    }
}
