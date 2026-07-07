package com.mileway.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

// Geometry: a 120dp halo box (100dp compact) wraps a 100dp button disc (84dp compact).
private val HaloSize = 120.dp
private val HaloSizeCompact = 100.dp
private val ButtonSize = 100.dp
private val ButtonSizeCompact = 84.dp
private val IconSize = 32.dp
private val GlowElevation = 12.dp

/** Container/content colors cross-fade over 300ms when the button switches state. */
private const val COLOR_ANIM_MILLIS = 300

/** Scale target while pressed (and, in the stateful overload, while a trip is active). */
private const val PRESSED_SCALE = 0.95f

/** Alpha applied to the container color for the ambient/spot glow shadow. */
private const val GLOW_ALPHA = 0.3f

/**
 * Hero circular action button for the trip tracking screen.
 *
 * A circular disc with an icon stacked above a bold label, surrounded by a soft
 * outer glow tinted with the container color. The disc springs down to 95% scale
 * while pressed and fires a haptic tick on tap. When disabled it mutes to the
 * surface-variant palette and ignores taps.
 *
 * @param text Label rendered below the icon (e.g. "START" or "STOP").
 * @param icon Icon rendered above the label, 32dp.
 * @param onClick Invoked on tap (after haptic feedback) when [enabled].
 * @param isCompact Use the smaller 84dp disc for short windows or large font scales.
 * @param enabled When false the button is visually muted and not clickable.
 * @param containerColor Disc color override; defaults to the primary color.
 * @param contentColor Icon/label color override; defaults to the on-primary color.
 */
@Composable
fun TrackMilesMainActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val haptic = LocalHapticFeedback.current

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(COLOR_ANIM_MILLIS),
        label = "buttonColor",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(COLOR_ANIM_MILLIS),
        label = "buttonContentColor",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale",
    )

    Box(
        modifier =
            modifier
                .size(if (isCompact) HaloSizeCompact else HaloSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow: transparent circle casting a tinted shadow around the disc.
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = GlowElevation,
                        shape = DesignTokens.Shape.button,
                        ambientColor = animatedContainerColor.copy(alpha = GLOW_ALPHA),
                        spotColor = animatedContainerColor.copy(alpha = GLOW_ALPHA),
                    ),
            shape = DesignTokens.Shape.button,
            color = Color.Transparent,
        ) {}

        // Main button disc
        Surface(
            modifier =
                Modifier
                    .size(if (isCompact) ButtonSizeCompact else ButtonSize)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        enabled = enabled,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
            shape = DesignTokens.Shape.button,
            color = if (enabled) animatedContainerColor else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(IconSize),
                    tint = if (enabled) animatedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                Text(
                    text = text,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = if (enabled) animatedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Tracking-state convenience overload.
 *
 * Maps tracking state to the canonical START/STOP presentation:
 * - Not tracking → "START", play icon, primary/onPrimary, gated by [isStartEnabled].
 * - Tracking (paused or not) → "STOP", stop icon, error/onError, gated by [isStopEnabled].
 *   A paused journey is still in progress, so the hero button keeps showing STOP;
 *   pause/resume is handled by a separate control.
 *
 * While tracking, the whole button additionally rests at 95% scale via a
 * medium-bouncy spring, on top of the press scale.
 */
@Composable
fun TrackMilesMainActionButton(
    isTracking: Boolean,
    isPaused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    isStartEnabled: Boolean = true,
    isStopEnabled: Boolean = true,
) {
    // A paused journey is still in progress, so any active trip shows STOP
    // regardless of isPaused.
    val isStopState =
        when {
            isTracking && isPaused -> true
            isTracking && !isPaused -> true
            else -> false
        }

    val trackingScale by animateFloatAsState(
        targetValue = if (isTracking) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "trackingScale",
    )

    TrackMilesMainActionButton(
        text = if (isStopState) "STOP" else "START",
        icon = if (isStopState) Icons.Filled.Stop else Icons.Filled.PlayArrow,
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = trackingScale
                scaleY = trackingScale
            },
        isCompact = isCompact,
        enabled = if (isStopState) isStopEnabled else isStartEnabled,
        containerColor =
            if (isStopState) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        contentColor =
            if (isStopState) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
    )
}
