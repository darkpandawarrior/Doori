package com.mileway.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_close
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/** Maximum zoom level for pinch gestures. */
private const val MAX_SCALE = 5f

/** Minimum zoom level (fit-to-screen). */
private const val MIN_SCALE = 1f

/** Zoom level toggled in by a double tap. */
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * A fullscreen image viewer dialog with zoom and pan capabilities.
 *
 * - Pinch to zoom, clamped between 1x and 5x.
 * - Double tap toggles between 1x and 2.5x.
 * - Drag to pan when zoomed, clamped so the image cannot be flung off screen.
 * - Single tap toggles the close-button visibility; gestures hide it.
 *
 * @param painter Painter for the image to display (caller resolves the image)
 * @param contentDescription Accessibility description for the image
 * @param onDismiss Called when the user dismisses the viewer
 */
@Composable
fun ZoomImageViewer(
    painter: Painter,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Track whether the controls should be visible (hide after interaction)
    var showControls by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = {
                                if (scale > MIN_SCALE) {
                                    scale = MIN_SCALE
                                    offset = Offset.Zero
                                } else {
                                    scale = DOUBLE_TAP_SCALE
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { _, pan, zoom, _ ->
                                // Apply zoom constraints (min 1x, max 5x)
                                scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)

                                if (scale > MIN_SCALE) {
                                    // Apply offset, clamped so the image stays within bounds
                                    val maxX = (size.width * (scale - 1f)) / 2f
                                    val maxY = (size.height * (scale - 1f)) / 2f
                                    offset =
                                        Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                                        )
                                } else {
                                    // Reset offset when not zoomed
                                    offset = Offset.Zero
                                }

                                // Hide controls during interaction
                                showControls = false
                            },
                        )
                    },
        ) {
            // Image with zoom/pan behavior
            Image(
                painter = painter,
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

            // Close button with fade in/out effect
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .alpha(if (showControls) 1f else 0f),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = DesignTokens.Shape.button,
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.core_cd_close),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
