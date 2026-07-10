package com.mileway.feature.profile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_permission_activity_recognition
import com.mileway.core.ui.resources.profile_permission_bluetooth
import com.mileway.core.ui.resources.profile_permission_camera
import com.mileway.core.ui.resources.profile_permission_location_background
import com.mileway.core.ui.resources.profile_permission_location_precise
import com.mileway.core.ui.resources.profile_permission_notifications
import com.mileway.core.ui.resources.profile_permission_storage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V22 P6.7: one row of Settings' "Permission Health" section, plus the Android permission
 * string [computeHealth] needs to actually check with `ContextCompat.checkSelfPermission`. Storage
 * has no runtime permission from API 30+ onward (scoped storage) — it's rendered as always-granted
 * rather than checking a permission string that no longer gates anything on this app's `minSdk`.
 *
 * [key] is a stable, non-localized identifier used for lookup ([manifestPermissionFor]); the
 * user-visible label is resolved from [labelRes] at composition time.
 */
private data class PlatformPermissionEntry(
    val key: String,
    val labelRes: StringResource,
    val icon: ImageVector,
    val isRequired: Boolean,
    /** `null` = nothing to check on this API level (e.g. Storage on scoped-storage devices). */
    val manifestPermission: String?,
)

private val PLATFORM_PERMISSIONS =
    buildList {
        add(
            PlatformPermissionEntry(
                "location_precise",
                Res.string.profile_permission_location_precise,
                Icons.Filled.LocationOn,
                isRequired = true,
                manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            ),
        )
        add(
            PlatformPermissionEntry(
                "location_background",
                Res.string.profile_permission_location_background,
                Icons.Filled.PinDrop,
                isRequired = true,
                manifestPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    } else {
                        null
                    },
            ),
        )
        add(
            PlatformPermissionEntry(
                "camera",
                Res.string.profile_permission_camera,
                Icons.Filled.Camera,
                isRequired = true,
                manifestPermission = Manifest.permission.CAMERA,
            ),
        )
        add(
            PlatformPermissionEntry(
                "storage",
                Res.string.profile_permission_storage,
                Icons.Filled.Folder,
                isRequired = true,
                manifestPermission = null,
            ),
        )
        add(
            PlatformPermissionEntry(
                "notifications",
                Res.string.profile_permission_notifications,
                Icons.Filled.NotificationsNone,
                isRequired = false,
                manifestPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.POST_NOTIFICATIONS
                    } else {
                        null
                    },
            ),
        )
        add(
            PlatformPermissionEntry(
                "activity_recognition",
                Res.string.profile_permission_activity_recognition,
                Icons.AutoMirrored.Filled.DirectionsRun,
                isRequired = false,
                manifestPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Manifest.permission.ACTIVITY_RECOGNITION
                    } else {
                        null
                    },
            ),
        )
        add(
            PlatformPermissionEntry(
                "bluetooth",
                Res.string.profile_permission_bluetooth,
                Icons.Filled.Bluetooth,
                isRequired = false,
                manifestPermission = null,
            ),
        )
    }

internal data class PermissionHealthRow(
    val key: String,
    val name: String,
    val icon: ImageVector,
    val isRequired: Boolean,
    val isGranted: Boolean,
)

/**
 * Re-checks every [PLATFORM_PERMISSIONS] entry against real device state on first composition and
 * on every `ON_RESUME` (so revoking a permission from system Settings and returning to this screen
 * updates the ring, per this task's acceptance clause), instead of the previous hardcoded
 * `isGranted = true/false` list.
 */
@Composable
internal fun rememberPermissionHealthRows(): List<PermissionHealthRow> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val labels = PLATFORM_PERMISSIONS.associate { it.key to stringResource(it.labelRes) }

    fun snapshot(): List<PermissionHealthRow> =
        PLATFORM_PERMISSIONS.map { entry ->
            val granted =
                entry.manifestPermission == null ||
                    ContextCompat.checkSelfPermission(context, entry.manifestPermission) ==
                    PackageManager.PERMISSION_GRANTED
            PermissionHealthRow(
                key = entry.key,
                name = labels.getValue(entry.key),
                icon = entry.icon,
                isRequired = entry.isRequired,
                isGranted = granted,
            )
        }

    var rows by remember { mutableStateOf(snapshot()) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    rows = snapshot()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return rows
}

/**
 * The runtime-requestable manifest permission string for a [PermissionHealthRow.key], or `null`
 * if this permission has nothing to request (not runtime-gated on this API level, e.g. Storage
 * under scoped storage) — those cases must fall back to the system app-details Settings screen
 * instead of calling `ActivityResultContracts.RequestPermission()` with a blank string.
 */
internal fun manifestPermissionFor(key: String): String? = PLATFORM_PERMISSIONS.firstOrNull { it.key == key }?.manifestPermission
