package com.mileway.feature.whatsnew.data

import com.mileway.feature.whatsnew.model.UpdateInfo
import com.mileway.feature.whatsnew.model.UpdateRequirement
import com.mileway.feature.whatsnew.model.UpdateUrgency
import com.mileway.feature.whatsnew.model.deriveRequirement
import com.siddharth.kmp.appshell.AppUpdateManager
import com.siddharth.kmp.appshell.UpdateAvailability
import com.siddharth.kmp.appshell.UpdateConfig
import com.siddharth.kmp.appshell.UpdateMode

/**
 * Combines the platform [AppUpdateManager] — already Play-Core-backed on Android (gms) / App Store
 * lookup on iOS / a no-op returning [UpdateAvailability.NotAvailable] everywhere else, injected via
 * `core/platform`'s `PlatformBindings` — with Mileway's own [UpdateInfo] (min-supported build,
 * release notes, store URL: none of that comes back from Play Core or the App Store lookup) to
 * derive the richer [UpdateRequirement].
 *
 * Deliberately **not** an expect/actual seam of its own: [AppUpdateManager] already *is* that seam
 * (real per-platform actuals, dispatched at runtime through DI, not at compile time) — wrapping it
 * in a second expect/actual would duplicate it for no gain, and this module can't touch the gms/
 * noGms flavor source sets that own the real Play-Core wiring anyway (`app/src/gms`, out of this
 * module's partition). [currentBuildCode] is a plain constructor param for the same reason
 * `ConfigProvider.getUpdateConfig()`'s values are — the caller (`:app`'s `BuildConfig.VERSION_CODE`,
 * iOS's `CFBundleVersion`) supplies it, no platform seam needed for reading one int.
 *
 * [info] is a suspend supplier, not fetched internally, so a future remote config source is a
 * one-line swap — same shape as [WhatsNewRepository] being "one implementation swap away from a
 * real data source."
 */
class UpdateChecker(
    private val appUpdateManager: AppUpdateManager,
    private val currentBuildCode: Int,
    private val info: suspend () -> UpdateInfo?,
) {
    /**
     * Null when [info] has nothing to compare against. Otherwise the derived [UpdateRequirement] —
     * except a non-FORCED urgency only surfaces once the store itself confirms an installable
     * update exists ([UpdateAvailability.Available]); a FORCED requirement always surfaces, because
     * being below the minimum supported build isn't something the store confirming or not changes.
     */
    suspend fun check(): UpdateRequirement? {
        val requirement = info()?.deriveRequirement(currentBuildCode) ?: return null
        if (requirement.urgency == UpdateUrgency.NONE || requirement.urgency == UpdateUrgency.FORCED) {
            return requirement
        }
        val config =
            UpdateConfig(
                enabled = true,
                mode = UpdateMode.FLEXIBLE,
                minSupportedVersionCode = requirement.info.minSupportedBuildCode.toLong(),
            )
        val storeConfirms = appUpdateManager.checkForUpdate(config) is UpdateAvailability.Available
        return if (storeConfirms) requirement else requirement.copy(urgency = UpdateUrgency.NONE)
    }

    /** True once a flexible update has finished downloading and is waiting for a restart. */
    suspend fun readyToRestart(): Boolean = appUpdateManager.checkForUpdate(UpdateConfig(enabled = true)) is UpdateAvailability.Downloaded

    /** Applies a downloaded flexible update (restarts the app to install it). */
    suspend fun completeFlexibleUpdate() = appUpdateManager.completeFlexibleUpdate()

    fun startUpdate(mode: UpdateMode) = appUpdateManager.startUpdate(mode)
}
