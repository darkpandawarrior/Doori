package com.miletracker.feature.tracking.service

import platform.Foundation.NSUserDefaults

/**
 * P-C.3: iOS UserDefaults backing for [ShutdownFlagStore].
 *
 * Swift side: call [IosShutdownFlagStoreKt.setIosShutdownFlag] from applicationWillTerminate
 * inside AppDelegate. The key is "shutdown_pending" in the standard user defaults domain.
 */
class IosShutdownFlagStore : ShutdownFlagStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun set() {
        defaults.setBool(true, KEY)
        defaults.synchronize()
    }

    override fun consumeAndClear(): Boolean {
        val wasSet = defaults.boolForKey(KEY)
        if (wasSet) {
            defaults.removeObjectForKey(KEY)
            defaults.synchronize()
        }
        return wasSet
    }

    companion object {
        private const val KEY = "shutdown_pending"
    }
}

/** Called from Swift AppDelegate.applicationWillTerminate. */
fun setIosShutdownFlag() {
    IosShutdownFlagStore().set()
}
