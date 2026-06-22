@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.media.ui.camera

import android.net.Uri
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.feature.media.model.FlashMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/** Side of the square focus ring drawn at the tap point. */
private val FocusRingSize = 72.dp

/**
 * Real CameraX preview + capture. Requests the CAMERA permission, binds a
 * [LifecycleCameraController] to a [PreviewView], and on capture writes a JPEG to
 * the app cache dir before handing the uri back to the caller.
 *
 * Interaction model:
 * - **Flash toggle** (top-right): cycles AUTO -> ON -> OFF via [onCycleFlash]; the icon
 *   reflects [flashMode] and the controller's `imageCaptureFlashMode` is kept in sync.
 * - **Pinch-to-zoom**: `detectTransformGestures` scales `cameraControl.setZoomRatio`,
 *   clamped to the device's reported zoom range.
 * - **Tap-to-focus**: a tap triggers a CameraX focus-metering action at the tap point and
 *   flashes a brief animated focus ring there.
 * - **Odometer mode** ([isOdometerMode]): overlays an alignment-guide rectangle with the
 *   "Align odometer digits within the frame" caption and a visual-only "Auto Capture: Off" pill.
 *
 * All new parameters default so the original `CameraCaptureScreen(onCaptured = ...)` call
 * sites keep compiling unchanged.
 *
 * @param onCaptured     invoked with the saved file uri string once a photo is taken
 * @param isOdometerMode when true, shows the odometer alignment overlay + caption
 * @param flashMode      current flash mode reflected by the toggle icon and controller
 * @param onCycleFlash   advances [flashMode]; defaults to a no-op for legacy call sites
 */
@Composable
fun CameraCaptureScreen(
    onCaptured: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOdometerMode: Boolean = false,
    flashMode: FlashMode = FlashMode.AUTO,
    onCycleFlash: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = rememberCameraPermissionState()
    val scope = rememberCoroutineScope()

    // Ask for the permission on first composition if we don't already have it.
    LaunchedEffect(Unit) {
        if (!permission.hasPermission) permission.request()
    }

    if (!permission.hasPermission) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Camera permission is required to capture a photo.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.l),
            )
            Button(onClick = permission.request) {
                Text("Grant permission")
            }
        }
        return
    }

    val controller =
        remember {
            LifecycleCameraController(context).apply {
                bindToLifecycle(lifecycleOwner)
            }
        }

    // Keep the CameraX flash mode in sync with the UI toggle.
    LaunchedEffect(flashMode) {
        controller.imageCaptureFlashMode =
            when (flashMode) {
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            }
    }

    // Animated focus ring state: where the last tap landed and its current alpha.
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAlpha = remember { Animatable(0f) }

    // Running pinch zoom ratio, clamped to the camera's reported range.
    var zoomRatio by remember { mutableStateOf(1f) }

    // D.1: exposure compensation. cameraInfo isn't observable, so poll briefly until the camera binds
    // and reports its EV capabilities, then drive the slider.
    var exposureIndex by remember { mutableStateOf(0) }
    var exposureRange by remember { mutableStateOf<IntRange?>(null) }
    LaunchedEffect(controller) {
        repeat(20) {
            val state = controller.cameraInfo?.exposureState
            if (state != null && state.isExposureCompensationSupported) {
                val range = state.exposureCompensationRange
                if (range.lower < range.upper) {
                    exposureRange = range.lower..range.upper
                    exposureIndex = state.exposureCompensationIndex
                    return@LaunchedEffect
                }
            }
            delay(150)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Pinch-to-zoom: scale the live zoom ratio within the device's bounds.
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            val bounds = controller.zoomState.value
                            val min = bounds?.minZoomRatio ?: 1f
                            val max = bounds?.maxZoomRatio ?: 1f
                            zoomRatio = (zoomRatio * gestureZoom).coerceIn(min, max)
                            controller.cameraControl?.setZoomRatio(zoomRatio)
                        }
                    }
                    // Tap-to-focus: meter at the tap point and flash a focus ring there.
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            focusPoint = tap
                            // A SurfaceOrientedMeteringPointFactory maps view px -> metering
                            // coordinates without needing the PreviewView instance here.
                            val pointFactory =
                                androidx.camera.core.SurfaceOrientedMeteringPointFactory(
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                )
                            val point = pointFactory.createPoint(tap.x, tap.y)
                            runCatching {
                                controller.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(point).build(),
                                )
                            }
                            scope.launch {
                                focusAlpha.snapTo(1f)
                                focusAlpha.animateTo(0f, animationSpec = tween(durationMillis = 900))
                            }
                        }
                    },
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                }
            },
        )

        // Odometer alignment guide: a centred rectangle with an instruction caption.
        if (isOdometerMode) {
            OdometerAlignmentOverlay(modifier = Modifier.fillMaxSize())
        }

        // Top controls: flash toggle (always) + an "Auto Capture: Off" pill in odometer mode.
        // displayCutoutPadding keeps controls clear of camera punch-holes / notches.
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isOdometerMode) {
                AutoCapturePill()
            } else {
                Spacer(Modifier.size(0.dp))
            }
            FlashToggleButton(flashMode = flashMode, onClick = onCycleFlash)
        }

        // Animated focus ring at the last tap point.
        focusPoint?.let { point ->
            FocusRing(
                center = point,
                alpha = focusAlpha.value,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // D.1: exposure-compensation slider, shown once the camera reports a usable EV range.
        exposureRange?.let { range ->
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 112.dp, start = DesignTokens.Spacing.xl, end = DesignTokens.Spacing.xl)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessMedium,
                    contentDescription = "Exposure",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { value ->
                        val idx = value.roundToInt().coerceIn(range.first, range.last)
                        exposureIndex = idx
                        runCatching { controller.cameraControl?.setExposureCompensationIndex(idx) }
                    },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first - 1).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FloatingActionButton(
            onClick = {
                val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                controller.takePicture(
                    output,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                            val uri = results.savedUri ?: Uri.fromFile(file)
                            onCaptured(uri.toString())
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Demo: surface failures silently; a real app would emit an event.
                        }
                    },
                )
            },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = DesignTokens.Spacing.xxl),
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Capture photo",
            )
        }
    }
}

/** Circular flash toggle whose icon reflects the current [FlashMode]. */
@Composable
private fun FlashToggleButton(
    flashMode: FlashMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon: ImageVector, label) =
        when (flashMode) {
            FlashMode.AUTO -> Icons.Default.FlashAuto to "Flash: auto"
            FlashMode.ON -> Icons.Default.FlashOn to "Flash: on"
            FlashMode.OFF -> Icons.Default.FlashOff to "Flash: off"
        }
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(DesignTokens.IconSize.actionTile),
            )
        }
    }
}

/** Visual-only "Auto Capture: Off" pill shown in odometer mode (parity with the source app). */
@Composable
private fun AutoCapturePill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.45f),
    ) {
        Text(
            text = "Auto Capture: Off",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.m,
                    vertical = DesignTokens.Spacing.xs,
                ),
        )
    }
}

/**
 * Centred alignment-guide rectangle for the odometer scan, with the
 * "Align odometer digits within the frame" caption inside the band.
 */
@Composable
private fun OdometerAlignmentOverlay(modifier: Modifier = Modifier) {
    val accent = DesignTokens.StatusColors.info
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.86f)
                    .clip(DesignTokens.Shape.roundedSm)
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(1.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Inner band that carries the caption and the accent border.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(DesignTokens.Shape.roundedSm)
                        .background(Color.Transparent)
                        .padding(vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Align odometer digits within the frame",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(
                                horizontal = DesignTokens.Spacing.m,
                                vertical = DesignTokens.Spacing.xs,
                            ),
                )
            }
            // Accent rectangle border drawn over the band.
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = accent,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                )
            }
        }
    }
}

/** Brief square focus ring drawn at [center] (px), fading out via [alpha]. */
@Composable
private fun FocusRing(
    center: Offset,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.alpha(alpha)) {
        val sidePx = FocusRingSize.toPx()
        val topLeft = Offset(center.x - sidePx / 2f, center.y - sidePx / 2f)
        drawRoundRect(
            color = accent,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(sidePx, sidePx),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
        )
    }
}
