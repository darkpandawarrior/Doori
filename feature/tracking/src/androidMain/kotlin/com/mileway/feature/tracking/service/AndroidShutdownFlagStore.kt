package com.mileway.feature.tracking.service

import android.content.SharedPreferences

/** P-C.3: Android SharedPreferences backing for [ShutdownFlagStore]. */
class AndroidShutdownFlagStore(private val prefs: SharedPreferences) : ShutdownFlagStore {
    override fun set() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    override fun consumeAndClear(): Boolean {
        val wasSet = prefs.getBoolean(KEY, false)
        if (wasSet) prefs.edit().remove(KEY).apply()
        return wasSet
    }

    companion object {
        private const val KEY = "shutdown_pending"
        const val PREFS_NAME = "tracking_flags"
    }
}
