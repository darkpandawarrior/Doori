package com.mileway.core.ui.components.topbar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth

/** Title font size when the bar is fully expanded. */
private val TitleExpandedFontSize = 22.sp

/** Title font size when the bar is fully collapsed. */
private val TitleCollapsedFontSize = 16.sp

/**
 * A navigation depth-aware top app bar implementing the "deeper = calmer" pattern:
 * ROOT renders a bold brand gradient, LEVEL_1 a solid accent, deeper levels a plain surface.
 *
 * Collapse-on-scroll: pass a Material 3 [TopAppBarScrollBehavior] (or an explicit
 * [collapsedFraction]) and the bar transitions from its depth styling when expanded to a
 * plain surface once the collapsed fraction crosses [collapseThreshold]. The title also
 * shrinks smoothly from 22sp to 16sp as the bar collapses. Without either parameter the
 * bar renders exactly as before (always expanded), so existing call sites are unaffected.
 *
 * @param title       primary title text
 * @param subtitle    optional secondary line shown under the title
 * @param depth       navigation depth controlling the styling (see [NavigationDepth])
 * @param navigationIcon optional leading slot (e.g. a back button)
 * @param scrollBehavior optional scroll behavior driving the collapse transition
 *                    (e.g. `TopAppBarDefaults.enterAlwaysScrollBehavior()`); also forwarded
 *                    to the underlying [TopAppBar] so height offsets apply
 * @param collapsedFraction optional explicit collapse fraction (0 = expanded, 1 = collapsed);
 *                    overrides [scrollBehavior]'s fraction when provided
 * @param collapseThreshold fraction above which the bar styles itself as collapsed
 * @param titleIcon   optional icon rendered before the title
 * @param titleIconContentDescription content description for [titleIcon]
 * @param animateTitleIcon when true, [titleIcon] pulses (scale 1.0 -> 1.08, 1800ms ease-in-out)
 * @param titleMaxWidth optional cap on the title area width (unconstrained when null)
 * @param showSearchAction when true, shows an animated circular search action before [actions]
 * @param onSearchClick callback for the search action
 * @param actionSpacing horizontal space between trailing action icons
 * @param actions     optional trailing action slot
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthAwareTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    depth: NavigationDepth = NavigationDepth.LEVEL_1,
    navigationIcon: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    collapsedFraction: Float? = null,
    collapseThreshold: Float = 0.3f,
    titleIcon: ImageVector? = null,
    titleIconContentDescription: String? = null,
    animateTitleIcon: Boolean = false,
    titleMaxWidth: Dp? = null,
    showSearchAction: Boolean = false,
    onSearchClick: () -> Unit = {},
    actionSpacing: Dp = DesignTokens.Spacing.s,
    actions: @Composable () -> Unit = {},
) {
    val config = DesignTokens.topBarConfig(depth)
    val colorScheme = MaterialTheme.colorScheme

    val fraction =
        (collapsedFraction ?: scrollBehavior?.state?.collapsedFraction ?: 0f)
            .coerceIn(0f, 1f)
    val isCollapsed = fraction > collapseThreshold

    // Collapsed: plain surface + on-surface content. Expanded: depth-aware styling.
    val titleColor = if (isCollapsed) colorScheme.onSurface else config.textColors.titleColor
    val subtitleColor =
        if (isCollapsed) colorScheme.onSurfaceVariant else config.textColors.subtitleColor
    val iconColor = if (isCollapsed) colorScheme.onSurface else config.textColors.iconColor
    val containerColor = if (isCollapsed) colorScheme.surface else Color.Transparent

    val backgroundModifier =
        if (isCollapsed) {
            Modifier
        } else if (config.useGradient && config.gradientBrush != null) {
            Modifier.background(config.gradientBrush)
        } else {
            Modifier.background(config.containerColor)
        }

    // Title shrinks 22sp -> 16sp tracking the collapse fraction.
    val titleFontSize =
        (
            TitleExpandedFontSize.value +
                (TitleCollapsedFontSize.value - TitleExpandedFontSize.value) * fraction
        ).sp

    // Pulsing title icon: scale 1.0 -> 1.08, 1800ms ease-in-out, reversing forever.
    val titleIconScale =
        if (titleIcon != null && animateTitleIcon) {
            val transition = rememberInfiniteTransition(label = "topBarIcon")
            transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "topBarIconScale",
            ).value
        } else {
            1f
        }

    // Whether the bar currently sits on a strong accent background (gradient or solid accent),
    // which calls for a translucent white search container instead of a surface tint.
    val onAccentBackground =
        !isCollapsed &&
            (depth == NavigationDepth.ROOT || depth == NavigationDepth.LEVEL_1)

    TopAppBar(
        modifier = modifier.then(backgroundModifier),
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                navigationIconContentColor = iconColor,
                titleContentColor = titleColor,
                actionIconContentColor = iconColor,
            ),
        navigationIcon = navigationIcon,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    if (titleMaxWidth != null) {
                        Modifier.sizeIn(maxWidth = titleMaxWidth)
                    } else {
                        Modifier
                    },
            ) {
                titleIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = titleIconContentDescription,
                        tint = titleColor,
                        modifier =
                            Modifier
                                .size(DesignTokens.IconSize.actionTile)
                                .scale(titleIconScale),
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.s))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 2,
                        )
                    }
                }
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = DesignTokens.Spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showSearchAction) {
                        AnimatedSearchAction(
                            onClick = onSearchClick,
                            tint = iconColor,
                            containerColor =
                                if (onAccentBackground) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                            showGlow = !isCollapsed,
                        )
                    }
                    actions()
                }
            }
        },
    )
}

/**
 * Animated circular search action used by [DepthAwareTopBar].
 *
 * Visual behaviour:
 * - subtle breathing pulse while idle (scale 1.0 -> 1.08, 2000ms ease-in-out, reversing)
 * - soft glow ring behind the container whose opacity cycles 0.3 -> 0.7 (1500ms linear)
 * - press feedback: scale to 0.88 plus an -8 degree tilt on a medium-bouncy spring
 * - haptic feedback on click
 */
@Composable
private fun AnimatedSearchAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    showGlow: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Infinite breathing animation (subtle scale pulse).
    val infiniteTransition = rememberInfiniteTransition(label = "searchBreathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathingScale",
    )

    // Glow opacity animation.
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowAlpha",
    )

    // Press scale animation.
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "pressScale",
    )

    // Press rotation animation (subtle tilt).
    val rotation by animateFloatAsState(
        targetValue = if (isPressed) -8f else 0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "pressRotation",
    )

    Box(
        modifier =
            modifier
                .size(48.dp)
                .scale(scale)
                .rotate(rotation),
        contentAlignment = Alignment.Center,
    ) {
        // Glow layer (blurred circle behind the container).
        if (showGlow) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp * breathingScale)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.25f),
                            shape = CircleShape,
                        )
                        .blur(12.dp),
            )
        }

        // Main circular container.
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(color = containerColor, shape = CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
            contentAlignment = Alignment.Center,
        ) {
            // Search glyph with the breathing pulse (paused while pressed).
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = tint,
                modifier =
                    Modifier
                        .size(DesignTokens.IconSize.actionTile)
                        .scale(if (!isPressed) breathingScale else 1f),
            )
        }
    }
}
