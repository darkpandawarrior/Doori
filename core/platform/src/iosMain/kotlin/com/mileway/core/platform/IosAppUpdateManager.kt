package com.mileway.core.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS in-app update via the public iTunes Lookup API (no backend, full parity with the Android Play-Core
 * path). Compares the App Store version to the running `CFBundleShortVersionString`; the shared UpdateGate
 * (UP.4) renders a dismissible prompt for FLEXIBLE or a blocking wall for FORCED. [startUpdate] opens the
 * App Store page. Any network/parse failure → [UpdateAvailability.NotAvailable], never a crash.
 */
class IosAppUpdateManager(
    private val bundleId: String? = NSBundle.mainBundle.bundleIdentifier,
    private val currentVersion: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String) ?: "0",
    private val httpClient: HttpClient = HttpClient(Darwin),
) : AppUpdateManager {
    private var appStoreId: String? = null

    override suspend fun checkForUpdate(config: UpdateConfig): UpdateAvailability {
        val id = bundleId
        if (!config.enabled || id.isNullOrBlank()) return UpdateAvailability.NotAvailable
        return runCatching {
            val body = httpClient.get("https://itunes.apple.com/lookup?bundleId=$id").bodyAsText()
            val storeVersion =
                VERSION_REGEX.find(body)?.groupValues?.getOrNull(1)
                    ?: return UpdateAvailability.NotAvailable
            appStoreId = TRACK_ID_REGEX.find(body)?.groupValues?.getOrNull(1)
            if (isNewer(storeVersion, currentVersion)) {
                UpdateAvailability.Available(availableVersionCode = 0L, mode = config.mode, priority = config.priority)
            } else {
                UpdateAvailability.NotAvailable
            }
        }.getOrDefault(UpdateAvailability.NotAvailable)
    }

    override fun startUpdate(mode: UpdateMode) {
        val id = appStoreId ?: return
        val url = NSURL.URLWithString("itms-apps://itunes.apple.com/app/id$id") ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
    }

    override suspend fun completeFlexibleUpdate() = Unit

    private companion object {
        val VERSION_REGEX = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"")
        val TRACK_ID_REGEX = Regex("\"trackId\"\\s*:\\s*(\\d+)")

        fun isNewer(
            store: String,
            current: String,
        ): Boolean {
            val storeParts = store.split(".")
            val currentParts = current.split(".")
            repeat(maxOf(storeParts.size, currentParts.size)) { i ->
                val s = storeParts.getOrNull(i)?.toIntOrNull() ?: 0
                val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (s != c) return s > c
            }
            return false
        }
    }
}
