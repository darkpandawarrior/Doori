package com.mileway.feature.media.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Lightweight CAMERA permission holder built on the AndroidX result APIs (no Accompanist).
 *
 * @param hasPermission  whether CAMERA is currently granted
 * @param request        launches the system permission dialog
 */
class CameraPermissionState internal constructor(
    val hasPermission: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            granted = isGranted
        }

    return CameraPermissionState(
        hasPermission = granted,
        request = { launcher.launch(Manifest.permission.CAMERA) },
    )
}
