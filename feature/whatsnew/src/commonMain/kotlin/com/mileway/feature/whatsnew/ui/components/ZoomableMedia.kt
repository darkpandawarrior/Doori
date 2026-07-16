package com.mileway.feature.whatsnew.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import coil3.compose.AsyncImage

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 3f
private const val DOUBLE_TAP_SCALE = 2f

/**
 * PLAN_V36 P4 — pinch-to-zoom (1x–3x) + pan for one [HeroCarousel] page. Same gesture shape as
 * `core/ui`'s [com.mileway.core.ui.components.ZoomImageViewer] (`detectTransformGestures` +
 * `graphicsLayer`, pan clamped to the zoomed bounds), but inline rather than a fullscreen
 * `Dialog`, driven by a raw image model instead of a resolved `Painter`, and with a narrower
 * range plus a double-tap 1x↔2x toggle (spec §5.2's Mileway addition on top of the reference).
 *
 * [resetKey] identifies the current carousel page — scale/offset are `remember`ed against it, so
 * swiping to a new page always starts unzoomed (spec: "zoom resets when the page changes").
 */
@Composable
fun ZoomableMedia(
    model: Any?,
    contentDescription: String?,
    resetKey: Any?,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    var scale by remember(resetKey) { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }

    fun applyScale(target: Float) {
        val wasZoomed = scale > MIN_SCALE
        scale = target.coerceIn(MIN_SCALE, MAX_SCALE)
        val isZoomed = scale > MIN_SCALE
        if (isZoomed && !wasZoomed) {
            // Haptic fires once, right as a pinch/double-tap crosses out of the 1x resting state —
            // not on every gesture frame.
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
        if (isZoomed != wasZoomed) onZoomChanged(isZoomed)
        if (!isZoomed) offset = Offset.Zero
    }

    Box(
        modifier =
            modifier
                .pointerInput(resetKey) {
                    detectTapGestures(
                        onDoubleTap = { applyScale(if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE) },
                    )
                }
                .pointerInput(resetKey) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        applyScale(scale * zoom)
                        if (scale > MIN_SCALE) {
                            val maxX = (size.width * (scale - 1f)) / 2f
                            val maxY = (size.height * (scale - 1f)) / 2f
                            offset =
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                        }
                    }
                },
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
        )
    }
}
