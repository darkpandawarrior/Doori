package com.miletracker.core.ui.components.topbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.MilewayColors

/**
 * The live state communicated by [TrackingStatusPill] / [TrackingTopBar].
 *
 * Drives the pill's colour, label and animation: only [TRACKING] breathes and glows.
 */
enum class TrackingStatus {
    /** A journey is being recorded. Matrix-green pill with a soft breathing glow. */
    TRACKING,

    /** Recording is paused. Amber pill, steady (no glow). */
    PAUSED,

    /** Nothing is being recorded. Muted neutral pill, steady. */
    IDLE,
}

/**
 * A compact status pill, designed to sit inline in a top-bar title row.
 *
 * Anatomy: a small status dot + an upper-cased, letter-spaced label inside a rounded-50 [Surface]
 * tinted with the status colour at low alpha. The colour and label animate as the status changes
 * ([animateColorAsState], 400 ms). While [TrackingStatus.TRACKING] the whole pill breathes (subtle
 * alpha pulse, 1500 ms ease-in-out) and the dot carries a soft same-colour glow — the only place
 * a glow appears, per the matrix "glow on active/primary only" rule.
 *
 * Colours come entirely from the matrix tokens: [MilewayColors.success] (green), [MilewayColors.warning]
 * (amber), [MilewayColors.neutral] (idle). No hard-coded hexes; stays coherent across all four themes.
 */
@Composable
fun TrackingStatusPill(
    status: TrackingStatus,
    modifier: Modifier = Modifier,
) {
    val targetColor =
        when (status) {
            TrackingStatus.TRACKING -> MilewayColors.success
            TrackingStatus.PAUSED -> MilewayColors.warning
            TrackingStatus.IDLE -> MilewayColors.neutral
        }
    val label =
        when (status) {
            TrackingStatus.TRACKING -> "TRACKING"
            TrackingStatus.PAUSED -> "PAUSED"
            TrackingStatus.IDLE -> "IDLE"
        }
    val color by animateColorAsState(targetColor, tween(400), label = "pillColor")

    // Breathing pulse: only while actively tracking, so a live journey is unmistakable.
    val pulseAlpha =
        if (status == TrackingStatus.TRACKING) {
            val transition = rememberInfiniteTransition(label = "pillPulse")
            transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.72f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "pillPulseAlpha",
            ).value
        } else {
            1f
        }

    Surface(
        modifier = modifier.graphicsLayer { alpha = pulseAlpha },
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusDot(color = color, glow = status == TrackingStatus.TRACKING)
            Text(
                text = label,
                color = color,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                    ),
                maxLines = 1,
            )
        }
    }
}

/** An 8 dp status dot; when [glow] is set it sits over a soft blurred halo of the same [color]. */
@Composable
private fun StatusDot(
    color: Color,
    glow: Boolean,
) {
    Box(contentAlignment = Alignment.Center) {
        if (glow) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .blur(6.dp)
                        .background(color.copy(alpha = 0.55f), CircleShape),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
        )
    }
}

/**
 * A minimal, matrix-themed top bar for the active-tracking surface.
 *
 * Replaces the off-brand bold gradient header: the bar paints the plain theme [surface] (no colour
 * band), shows the screen [title] with the [TrackingStatusPill] embedded directly in the title row,
 * and hangs sync / network / overflow [actions] on the right. Status is communicated by the pill (and
 * the live hero card), not by a coloured chrome band — keeping the deep-dark theme calm and on-brand.
 *
 * @param title the screen title.
 * @param status the current tracking status driving the embedded pill.
 * @param modifier applied to the underlying [TopAppBar].
 * @param containerColor the bar background; defaults to the theme surface (never the brand gradient).
 * @param actions trailing action slot (sync / network-status / overflow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingTopBar(
    title: String,
    status: TrackingStatus,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    actions: @Composable () -> Unit = {},
) {
    // A hair-line bottom border separates the bar from the content without a colour band; it warms
    // to the accent while tracking (glow-on-active only) and stays a calm outline otherwise.
    val borderColor =
        if (status == TrackingStatus.TRACKING) {
            MilewayColors.glow
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        }

    TopAppBar(
        modifier =
            modifier.drawBehind {
                drawRect(
                    color = borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                )
            },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TrackingStatusPill(status = status)
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = DesignTokens.Spacing.xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                ) {
                    actions()
                    Spacer(modifier = Modifier.width(0.dp))
                }
            }
        },
    )
}
