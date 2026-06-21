@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.miletracker.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Default sub-status messages cycled beneath the headline while work is in flight.
 * Matches the production loader's "Preparing… → Syncing… → Finalizing…" cadence.
 */
val DefaultLoadingSubStatuses: List<String> =
    listOf(
        "Preparing…",
        "Syncing…",
        "Finalizing…",
    )

/** How long each sub-status stays on screen before the next is shown. */
private const val SubStatusDwellMillis = 1_600L

/**
 * Full-screen loading view: a centred, gently floating paper-plane mark above a
 * fixed headline and a cycling sub-status line.
 *
 * The plane is drawn entirely in Compose (no asset dependency) so the screen is
 * self-contained and previewable: a blue plane with a soft fold highlight,
 * trailing alongside a small grey cloud, bobbing on a slow loop. The sub-status
 * text advances through [subStatuses] every ~1.6s and cross-fades on change.
 *
 * Stateless: pass [message] and [subStatuses] in; there are no callbacks because
 * a loading screen has no user actions. The caller decides when to show/hide it.
 *
 * @param message Headline shown under the mark (e.g. "Working on your request…").
 * @param subStatuses Sub-status lines cycled in order; single-entry lists stay static.
 * @param modifier Applied to the root full-screen surface.
 */
@Composable
fun TrackLoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Working on your request…",
    subStatuses: List<String> = DefaultLoadingSubStatuses,
) {
    // Advance the sub-status index on a fixed dwell; pauses naturally for a
    // single-entry list (the loop body is a no-op cycle).
    var subIndex by remember(subStatuses) { mutableIntStateOf(0) }
    LaunchedEffect(subStatuses) {
        if (subStatuses.size <= 1) return@LaunchedEffect
        while (true) {
            delay(SubStatusDwellMillis)
            subIndex = (subIndex + 1) % subStatuses.size
        }
    }
    val currentSub = subStatuses.getOrNull(subIndex).orEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FloatingPaperPlane(
                    modifier = Modifier.size(160.dp),
                    planeColor = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(48.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                Spacer(Modifier.height(20.dp))

                // Cross-fade the sub-status text whenever the index advances.
                AnimatedContent(
                    targetState = currentSub,
                    transitionSpec = {
                        (fadeIn(tween(400)) togetherWith fadeOut(tween(400)))
                    },
                    label = "loadingSubStatus",
                ) { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
    }
}

/**
 * A Compose-drawn paper-plane that bobs up and down on a slow infinite loop, with
 * a small grey cloud just ahead of it. Pure drawing, no resources required.
 */
@Composable
private fun FloatingPaperPlane(
    modifier: Modifier = Modifier,
    planeColor: Color,
) {
    val transition = rememberInfiniteTransition(label = "paperPlane")

    // Vertical bob: a gentle ±1 oscillation, eased to a sine-like glide via the
    // reversing repeat mode.
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "bob",
    )

    // Subtle forward drift so the plane reads as "in flight" rather than static.
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2_600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "drift",
    )

    val foldHighlight = planeColor.copy(alpha = 0.55f)
    val cloudColor = Color(0xFF9AA0A6)

    Canvas(
        modifier =
            modifier.graphicsLayer {
                translationY = bob * 6f
                translationX = drift * 4f
            },
    ) {
        drawCloud(cloudColor)
        drawPaperPlane(planeColor, foldHighlight)
    }
}

/**
 * Draws a stylised paper plane pointing up-right, occupying roughly the
 * lower-left of the canvas. Two triangular wings plus a darker fold give it the
 * familiar origami look from the reference.
 */
private fun DrawScope.drawPaperPlane(
    planeColor: Color,
    foldHighlight: Color,
) {
    val w = size.width
    val h = size.height

    // Anchor points expressed as fractions of the canvas so the plane scales.
    val nose = Offset(w * 0.74f, h * 0.42f) // tip, up and to the right
    val tailTop = Offset(w * 0.30f, h * 0.40f) // upper rear corner
    val tailBottom = Offset(w * 0.34f, h * 0.66f) // lower rear corner
    val belly = Offset(w * 0.46f, h * 0.56f) // centre crease point

    // Upper wing (lighter underside catches the light).
    val upperWing =
        Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(tailTop.x, tailTop.y)
            lineTo(belly.x, belly.y)
            close()
        }
    drawPath(upperWing, color = foldHighlight, style = Fill)

    // Lower wing (the main body colour).
    val lowerWing =
        Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(belly.x, belly.y)
            lineTo(tailBottom.x, tailBottom.y)
            close()
        }
    drawPath(lowerWing, color = planeColor, style = Fill)

    // Central fold spine for a touch of depth.
    val spine =
        Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(belly.x, belly.y)
            lineTo((tailTop.x + tailBottom.x) / 2f, (tailTop.y + tailBottom.y) / 2f)
            close()
        }
    drawPath(spine, color = planeColor.copy(alpha = 0.85f), style = Fill)
}

/** Draws a small, soft three-lobe grey cloud in the upper-right of the canvas. */
private fun DrawScope.drawCloud(cloudColor: Color) {
    val w = size.width
    val h = size.height
    val r = w * 0.06f

    drawCircle(cloudColor, radius = r, center = Offset(w * 0.78f, h * 0.30f))
    drawCircle(cloudColor, radius = r * 1.3f, center = Offset(w * 0.86f, h * 0.27f))
    drawCircle(cloudColor, radius = r, center = Offset(w * 0.93f, h * 0.31f))
    // Flat base so the lobes read as one cloud.
    drawRect(
        color = cloudColor,
        topLeft = Offset(w * 0.78f, h * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.16f, r * 0.9f),
    )
}
