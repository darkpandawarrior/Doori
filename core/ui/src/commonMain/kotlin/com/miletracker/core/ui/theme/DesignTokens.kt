package com.miletracker.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Design tokens for Compose UI.
 * Centralizes spacing, shapes, elevations, icon/tile sizes, status colors and the
 * navigation-depth top bar styling so every screen shares one visual language.
 *
 * Trimmed to the tokens this demo actually uses
 * (SaaS-only approval/travel/benefit palettes dropped).
 *
 * Usage guidelines:
 * - Always consume tokens instead of hard-coded dp values to ensure consistency.
 * - Prefer MaterialTheme for dynamic color adaptation; tokens complement it.
 */
object DesignTokens {
    /** Spacing scale based on 4dp increments. */
    object Spacing {
        val xs = 4.dp
        val s = 8.dp
        val m = 12.dp
        val l = 16.dp
        val xl = 24.dp
        val xxl = 32.dp

        /** Standard horizontal padding for screen content */
        val screenHorizontal = 16.dp

        /** Spacing between carousel items */
        val carouselSpacing = 12.dp

        /** Vertical spacing between sections */
        val sectionSpacing = 24.dp
    }

    /** Corner radius scale for cards, chips and sheets. */
    object Shape {
        val roundedSm = RoundedCornerShape(12.dp)
        val roundedMd = RoundedCornerShape(16.dp)
        val roundedLg = RoundedCornerShape(20.dp)
        val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

        /** Square rounded sheet shape - more squared corners with slight rounding */
        val sheetSquared = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

        /** Shape for action tiles */
        val actionTile = RoundedCornerShape(14.dp)

        /** Shape for chips */
        val chip = RoundedCornerShape(14.dp)

        /** Shape for carousel cards */
        val carouselCard = RoundedCornerShape(18.dp)
    }

    /** Elevation scale for surfaces. */
    object Elevation {
        val card = 2.dp
        val raised = 4.dp
        val prominent = 8.dp
    }

    /**
     * Standardized card dimensions for header sections.
     */
    object CardSize {
        /** Header height for cards with gradient headers */
        val gradientHeaderHeight = 100.dp

        /** Compact header height */
        val compactHeaderHeight = 56.dp
    }

    /**
     * Icon sizes for consistent iconography across components.
     */
    object IconSize {
        /** Small inline icons */
        val inline = 16.dp

        /** Standard badge icons */
        val badge = 18.dp

        /** Action tile icons (inside container) */
        val actionTile = 22.dp

        /** Large action tile icons (circular style) */
        val actionTileLarge = 26.dp

        /** Navigation/quick link icons */
        val navigation = 20.dp

        /** Section header icons */
        val header = 24.dp
    }

    /**
     * Action tile container sizes for action grids.
     */
    object ActionTileSize {
        /** Default square tile container */
        val defaultContainer = 52.dp

        /** Circular tile container */
        val circularContainer = 56.dp

        /** Compact tile container */
        val compactContainer = 44.dp

        /** Default tile width including label */
        val defaultWidth = 72.dp

        /** Circular tile width */
        val circularWidth = 70.dp
    }

    /**
     * Status colors for badges, indicators, and alerts.
     *
     * These are the *static* (non-composable) fallbacks, kept in lock-step with the
     * Design Language v2 dark-surface tokens in [com.miletracker.core.ui.theme.MilewayColors].
     * Inside a composable, prefer `MilewayColors.success/warning/danger/info/neutral` — those
     * are theme-aware and stay AA on the light Daybreak surface too. Reach here only from
     * non-composable code (data classes, previews) that genuinely can't read the theme.
     */
    object StatusColors {
        val success = Color(0xFF00FF41) // Phosphor green - approved, active, completed
        val warning = Color(0xFFF2C14E) // Amber - pending, low balance
        val error = Color(0xFFF2545B) // Red - rejected, critical, overdue
        val info = Color(0xFF5BA8F5) // Blue - informational, processing
        val neutral = Color(0xFF9AA5A0) // Gray - draft, inactive

        /** Badge background color for counts */
        val badgeRed = Color(0xFFF2545B)
    }

    /**
     * Navigation depth levels for progressive visual simplification.
     *
     * Design Philosophy: "Deeper = Calmer"
     * As users navigate deeper into the app, the UI becomes progressively plainer,
     * helping them understand their navigation depth visually.
     *
     * - ROOT: Bold, vibrant gradient - the anchor point
     * - LEVEL_1: Solid accent color - calmer, no gradient
     * - LEVEL_2: Subtle surface tint - even calmer
     * - LEVEL_3+: Plain surface - minimal, content-focused
     */
    enum class NavigationDepth {
        ROOT,
        LEVEL_1,
        LEVEL_2,
        LEVEL_3_PLUS,
    }

    /**
     * Gradient brush for prominent top bars (ROOT level only).
     * For deeper levels, use [topBarContainerColor] instead of gradients.
     */
    @Composable
    fun topBarGradientBrush(): Brush {
        val colorScheme = MaterialTheme.colorScheme
        val isLight = colorScheme.isLight()

        val primary = colorScheme.primary
        val secondary = colorScheme.secondary
        val primaryContainer = colorScheme.primaryContainer

        val end =
            if (isLight) {
                lerpColor(secondary, primaryContainer, 0.25f)
            } else {
                secondary
            }

        return Brush.horizontalGradient(listOf(primary, end))
    }

    /**
     * Navigation depth-aware container color for top bars.
     * Implements progressive simplification: deeper navigation = plainer colors.
     */
    @Composable
    fun topBarContainerColor(depth: NavigationDepth): Color {
        val colorScheme = MaterialTheme.colorScheme
        val isLight = colorScheme.isLight()

        return when (depth) {
            NavigationDepth.ROOT -> Color.Transparent

            NavigationDepth.LEVEL_1 -> {
                if (isLight) {
                    colorScheme.secondary
                } else {
                    colorScheme.secondary.copy(alpha = 0.95f)
                }
            }

            NavigationDepth.LEVEL_2 -> {
                if (isLight) {
                    lerpColor(colorScheme.surfaceVariant, colorScheme.primaryContainer, 0.2f)
                } else {
                    lerpColor(colorScheme.surfaceVariant, colorScheme.primary, 0.15f)
                }
            }

            NavigationDepth.LEVEL_3_PLUS -> colorScheme.surface
        }
    }

    /** Only ROOT level uses a gradient background; deeper levels use solid colors. */
    fun shouldUseGradient(depth: NavigationDepth): Boolean = depth == NavigationDepth.ROOT

    /**
     * Complete TopBar configuration for a given navigation depth.
     */
    @Composable
    fun topBarConfig(depth: NavigationDepth): TopBarConfig {
        return TopBarConfig(
            depth = depth,
            useGradient = shouldUseGradient(depth),
            containerColor = topBarContainerColor(depth),
            gradientBrush = if (shouldUseGradient(depth)) topBarGradientBrush() else null,
            textColors = topBarTextColors(depth),
        )
    }

    data class TopBarConfig(
        val depth: NavigationDepth,
        val useGradient: Boolean,
        val containerColor: Color,
        val gradientBrush: Brush?,
        val textColors: TopBarTextColors,
    )

    /**
     * Text colors for navigation depth-aware top bars.
     * Adapts to the background style at each depth level.
     */
    @Composable
    fun topBarTextColors(depth: NavigationDepth): TopBarTextColors {
        val colorScheme = MaterialTheme.colorScheme

        return when (depth) {
            NavigationDepth.ROOT ->
                TopBarTextColors(
                    titleColor = Color.White,
                    subtitleColor = Color.White.copy(alpha = 0.85f),
                    iconColor = Color.White,
                )
            NavigationDepth.LEVEL_1 ->
                TopBarTextColors(
                    titleColor = colorScheme.onSecondary,
                    subtitleColor = colorScheme.onSecondary.copy(alpha = 0.8f),
                    iconColor = colorScheme.onSecondary,
                )
            NavigationDepth.LEVEL_2,
            NavigationDepth.LEVEL_3_PLUS,
            ->
                TopBarTextColors(
                    titleColor = colorScheme.onSurface,
                    subtitleColor = colorScheme.onSurfaceVariant,
                    iconColor = colorScheme.onSurface,
                )
        }
    }

    data class TopBarTextColors(
        val titleColor: Color,
        val subtitleColor: Color,
        val iconColor: Color,
    )
}

/** Returns whether the current Material color scheme is considered light. */
private fun androidx.compose.material3.ColorScheme.isLight(): Boolean {
    return this.surface.luminance() > this.onSurface.luminance()
}

/** Linear interpolate two colors (ARGB). */
private fun lerpColor(
    start: Color,
    end: Color,
    fraction: Float,
): Color {
    val a = start.alpha + (end.alpha - start.alpha) * fraction
    val r = start.red + (end.red - start.red) * fraction
    val g = start.green + (end.green - start.green) * fraction
    val b = start.blue + (end.blue - start.blue) * fraction
    return Color(r, g, b, a)
}

/** Compute relative luminance roughly for the light/dark heuristic. */
private fun Color.luminance(): Float {
    fun channel(c: Float): Float {
        return if (c <= 0.03928f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        }
    }
    return 0.2126f * channel(this.red) + 0.7152f * channel(this.green) + 0.0722f * channel(this.blue)
}
