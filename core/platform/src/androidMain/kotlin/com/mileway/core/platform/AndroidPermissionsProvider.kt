package com.mileway.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.siddharth.kmp.appshell.AppPermission
import com.siddharth.kmp.appshell.PermissionResult
import com.siddharth.kmp.appshell.PermissionsProvider

/**
 * PLAN_V35: Android [PermissionsProvider] — the binding was deferred at 2.2d and never landed,
 * which left `koinInject<PermissionsProvider>()` (TrackMilesScreen, MapScreen) CRASHING the whole
 * live-tracking flow on Android with NoDefinitionFoundException. (Found by run-verification;
 * iOS has had IosPermissionsProvider since C3.)
 *
 * [isGranted] is a plain [ContextCompat.checkSelfPermission] sweep. [request] needs an Activity
 * to show the system dialog, so the foreground activity registers a [requestBridge]
 * (ActivityResult-based — see LauncherActivity); with no bridge registered (headless/no UI host)
 * it degrades to reporting the current grant state instead of crashing.
 */
class AndroidPermissionsProvider(private val context: Context) : PermissionsProvider {
    /** Wired/cleared by the host activity. Launches the system dialog, resumes with grant map. */
    @Volatile
    var requestBridge: (suspend (Array<String>) -> Map<String, Boolean>)? = null

    override suspend fun isGranted(permission: AppPermission): Boolean = permission.manifestPermissions().all { granted(it) }

    override suspend fun request(permission: AppPermission): PermissionResult {
        val missing = permission.manifestPermissions().filterNot { granted(it) }
        if (missing.isEmpty()) return PermissionResult.Granted
        val bridge = requestBridge ?: return PermissionResult.Denied
        val grants = bridge(missing.toTypedArray())
        return if (grants.values.all { it }) PermissionResult.Granted else PermissionResult.Denied
    }

    private fun granted(manifestPermission: String): Boolean =
        ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED

    private fun AppPermission.manifestPermissions(): List<String> =
        when (this) {
            AppPermission.LOCATION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            AppPermission.LOCATION_BACKGROUND ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    emptyList() // pre-Q: background access rides on foreground location
                }
            AppPermission.CAMERA -> listOf(Manifest.permission.CAMERA)
            AppPermission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList() // pre-33: notifications are install-time granted
                }
            AppPermission.ACTIVITY_RECOGNITION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    emptyList()
                }
            AppPermission.STORAGE ->
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    emptyList() // 33+: media access goes through the photo picker / SAF, no runtime perm
                }
        }
}
