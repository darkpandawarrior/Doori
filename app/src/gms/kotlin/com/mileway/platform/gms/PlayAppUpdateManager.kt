package com.mileway.platform.gms

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory as PlayAppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.mileway.core.platform.AppUpdateManager
import com.mileway.core.platform.AppUpdateManagerFactory
import com.mileway.core.platform.UpdateAvailability
import com.mileway.core.platform.UpdateConfig
import com.mileway.core.platform.UpdateMode
import com.google.android.play.core.install.model.UpdateAvailability as PlayUpdateAvailability

/**
 * Play-Core in-app update (gms flavor only). Wraps Play's [PlayAppUpdateManagerFactory] delegate.
 *
 * Activity-scoped: the IntentSender launcher is registered through the host Activity's
 * [ComponentActivity.activityResultRegistry] (so it works even when constructed after RESUMED, which the
 * `registerForActivityResult` helper forbids). Every store call is wrapped in runCatching → degrades to
 * a no-op when there is no Play Store connection (e.g. a sideloaded demo build), never crashing.
 */
class PlayAppUpdateManager(activity: Activity) : AppUpdateManager {
    private val delegate = PlayAppUpdateManagerFactory.create(activity)
    private val launcher: ActivityResultLauncher<IntentSenderRequest>? =
        (activity as? ComponentActivity)?.activityResultRegistry?.register(
            "mileway_in_app_update",
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { /* flexible flow completion is observed via the install-state listener */ }

    private val installListener = InstallStateUpdatedListener { /* DOWNLOADED → UI calls completeFlexibleUpdate() */ }
    private var lastInfo: AppUpdateInfo? = null

    override suspend fun checkForUpdate(config: UpdateConfig): UpdateAvailability {
        if (!config.enabled) return UpdateAvailability.NotAvailable
        return runCatching {
            val info = delegate.requestAppUpdateInfo().also { lastInfo = it }
            if (info.updateAvailability() != PlayUpdateAvailability.UPDATE_AVAILABLE) {
                return UpdateAvailability.NotAvailable
            }
            val type = if (config.mode == UpdateMode.FORCED) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (!info.isUpdateTypeAllowed(type)) {
                return UpdateAvailability.NotAvailable
            }
            UpdateAvailability.Available(
                availableVersionCode = info.availableVersionCode().toLong(),
                mode = config.mode,
                priority = info.updatePriority(),
            )
        }.getOrDefault(UpdateAvailability.NotAvailable)
    }

    override fun startUpdate(mode: UpdateMode) {
        val info = lastInfo ?: return
        val activeLauncher = launcher ?: return
        val type = if (mode == UpdateMode.FORCED) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        runCatching {
            if (type == AppUpdateType.FLEXIBLE) delegate.registerListener(installListener)
            delegate.startUpdateFlowForResult(info, activeLauncher, AppUpdateOptions.defaultOptions(type))
        }
    }

    override suspend fun completeFlexibleUpdate() {
        runCatching { delegate.completeUpdate() }
    }
}

/** gms factory binding for [AppUpdateManagerFactory]. */
class PlayAppUpdateManagerFactoryImpl : AppUpdateManagerFactory {
    override fun create(activity: Activity): AppUpdateManager = PlayAppUpdateManager(activity)
}
