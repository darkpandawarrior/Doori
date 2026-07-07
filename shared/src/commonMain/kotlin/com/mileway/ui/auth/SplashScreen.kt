package com.mileway.ui.auth

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The staged status messages the splash cycles through while it "boots" the demo.
 * Each stage is shown for [STAGE_DURATION_MS] before advancing; after the last one the
 * screen reports completion via the supplied callback.
 */
private val SPLASH_STAGES =
    listOf(
        "Checking session…",
        "Syncing configuration…",
        "Preparing your workspace…",
    )

/** How long each [SPLASH_STAGES] message stays on screen. */
private const val STAGE_DURATION_MS = 700L

/**
 * Minimal centred launch screen shown before the app shell appears.
 *
 * Visual language mirrors the reference launch screen: a plain [MaterialTheme.colorScheme.background]
 * surface, a single centred logo mark (a compass needle drawn on [Canvas], no drawable resources),
 * and a small indeterminate spinner above staged status copy near the lower third.
 *
 * The screen owns its own insets via [Surface] (full-bleed background) and the content is
 * laid out with [fillMaxSize]; there are no pinned edge elements that need explicit inset
 * padding. The only state is the current animation stage, no ViewModel is required.
 *
 * @param onFinished invoked exactly once after the final stage has been displayed. The latest
 *   lambda is captured via [rememberUpdatedState] so a recomposition with a new callback does
 *   not restart the staged sequence.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    var stageIndex by remember { mutableIntStateOf(0) }

    // Drive the staged status copy. Keyed on Unit so it runs once across the screen's
    // lifetime; advancing the index in place keeps the AnimatedContent crossfade smooth.
    LaunchedEffect(Unit) {
        SPLASH_STAGES.indices.forEach { index ->
            stageIndex = index
            delay(STAGE_DURATION_MS)
        }
        currentOnFinished()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Centred logo mark, the visual anchor of the launch screen.
            CompassMark(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(96.dp),
            )

            // Spinner + staged status copy, weighted toward the lower third to match the
            // reference layout where the indicator sits well below the centred mark.
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                        .padding(bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                ArcSpinner(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(20.dp))
                AnimatedContent(
                    targetState = stageIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(220))
                    },
                    label = "splashStage",
                ) { index ->
                    Text(
                        text = SPLASH_STAGES.getOrElse(index) { SPLASH_STAGES.last() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * The shared app mark: a compass navigation needle drawn with two triangular halves, a solid
 * "north" pointer in [MaterialTheme.colorScheme.primary] and a faded "south" tail, mirroring
 * the neutral logo mark used in the bottom-bar FAB. Rendered entirely on [Canvas] so no drawable
 * resources are required.
 *
 * @param color base needle colour; the north half uses it at full strength and the south half
 *   at reduced alpha.
 */
@Composable
internal fun CompassMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val markDescription = "App logo"
    Canvas(
        modifier = modifier.semantics { contentDescription = markDescription },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        // Half-width of the needle at its waist; tuned so the needle reads as a slim compass arrow.
        val waist = w * 0.18f

        // North (bright) half: tip at top, waist at centre.
        val north =
            Path().apply {
                moveTo(cx, h * 0.06f)
                lineTo(cx + waist, cy)
                lineTo(cx, cy - h * 0.07f)
                lineTo(cx - waist, cy)
                close()
            }
        // South (faded) half: tail toward the bottom.
        val south =
            Path().apply {
                moveTo(cx, h * 0.94f)
                lineTo(cx - waist, cy)
                lineTo(cx, cy + h * 0.07f)
                lineTo(cx + waist, cy)
                close()
            }

        drawPath(path = north, color = color)
        drawPath(path = south, color = color.copy(alpha = 0.4f))
    }
}

/**
 * A small indeterminate spinner drawn as a single sweeping arc that rotates continuously.
 * Matches the slim arc indicator seen on the reference launch screen and avoids pulling in
 * a heavier progress component for this lightweight surface.
 */
@Composable
private fun ArcSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "arcSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "arcRotation",
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = size.minDimension * 0.12f
        val inset = strokeWidthPx / 2f
        rotate(degrees = rotation) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = size.width - strokeWidthPx,
                        height = size.height - strokeWidthPx,
                    ),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
        }
    }
}
