@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_retry
import com.mileway.core.ui.resources.core_cd_error
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// Shimmer loading skeletons
// =============================================================================

/**
 * Standardized shimmer specifications for consistency across all screens.
 */
object ShimmerSpecs {
    const val CARD_WIDTH = 280
    const val COMPACT_HEIGHT = 120 // For simple cards (approvals, simple lists)
    const val STANDARD_HEIGHT = 160 // For most carousel cards (transactions, vouchers)
    const val LARGE_HEIGHT = 200 // For complex cards with images (trips, bookings)
    const val HORIZONTAL_SPACING = 12
    const val CARD_COUNT = 3
    const val CORNER_RADIUS = 12
}

/**
 * Error state for a screen section.
 */
data class SectionError(
    val message: String,
    val canRetry: Boolean = true,
    val errorCode: String? = null,
)

/**
 * Load state of a screen section.
 */
data class SectionLoadState<T>(
    val data: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: SectionError? = null,
)

/**
 * Unified shimmer bar with customizable height and width fraction.
 * Theme-aware colors for dark-mode compliance.
 */
@Composable
fun ShimmerBar(
    height: Int = 14,
    widthFraction: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val colors =
        remember(surfaceVariant) {
            listOf(
                surfaceVariant.copy(alpha = 0.6f),
                surfaceVariant.copy(alpha = 0.3f),
                surfaceVariant.copy(alpha = 0.6f),
            )
        }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val xShimmer =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "x",
        )

    val brush =
        Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(xShimmer.value, xShimmer.value),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .height(height.dp)
                .clip(DesignTokens.Shape.button)
                .background(brush),
    )
}

/**
 * Unified shimmer card for loading states.
 *
 * @param height Height of the card in dp
 * @param showImagePlaceholder Whether to show an image placeholder at the top
 */
@Composable
fun UnifiedShimmerCard(
    height: Int = ShimmerSpecs.STANDARD_HEIGHT,
    showImagePlaceholder: Boolean = false,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        modifier =
            Modifier
                .width(ShimmerSpecs.CARD_WIDTH.dp)
                .height(height.dp),
        shape = DesignTokens.Shape.roundedSm,
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
    ) {
        Column {
            if (showImagePlaceholder) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            ),
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                ShimmerBar(height = 16, widthFraction = 0.5f)
                Spacer(Modifier.height(12.dp))
                ShimmerBar(height = 14, widthFraction = 0.8f)
                Spacer(Modifier.height(8.dp))
                ShimmerBar(height = 12, widthFraction = 0.6f)
                if (!showImagePlaceholder) {
                    Spacer(Modifier.height(8.dp))
                    ShimmerBar(height = 12, widthFraction = 0.4f)
                }
            }
        }
    }
}

/**
 * Unified carousel shimmer for horizontal scrolling sections.
 */
@Composable
fun UnifiedCarouselShimmer(
    height: Int = ShimmerSpecs.STANDARD_HEIGHT,
    cardCount: Int = ShimmerSpecs.CARD_COUNT,
    showImagePlaceholder: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ShimmerSpecs.HORIZONTAL_SPACING.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier,
    ) {
        items(cardCount) {
            UnifiedShimmerCard(
                height = height,
                showImagePlaceholder = showImagePlaceholder,
            )
        }
    }
}

/**
 * Compact shimmer for vertical list items.
 */
@Composable
fun UnifiedListShimmer(
    height: Int = 72,
    itemCount: Int = 5,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(itemCount) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height.dp)
                        .padding(horizontal = 16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                shape = DesignTokens.Shape.roundedSm,
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    DesignTokens.Shape.button,
                                ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBar(height = 14, widthFraction = 0.6f)
                        Spacer(Modifier.height(6.dp))
                        ShimmerBar(height = 12, widthFraction = 0.4f)
                    }
                    ShimmerBar(
                        height = 12,
                        widthFraction = 0.2f,
                        modifier = Modifier.width(60.dp),
                    )
                }
            }
        }
    }
}

/**
 * Error state component for sections with retry capability.
 */
@Composable
fun SectionErrorState(
    message: String,
    canRetry: Boolean = true,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Refresh,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            ),
        shape = DesignTokens.Shape.roundedSm,
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = DesignTokens.Shape.button,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(Res.string.core_cd_error),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (canRetry) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.height(36.dp),
                    shape = DesignTokens.Shape.button,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.action_retry), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// =============================================================================
// Dots indicator (carousel pager)
// =============================================================================

/**
 * Maximum number of visible dots; beyond this the dots cycle and a page badge shows.
 */
private const val MAX_VISIBLE_DOTS = 5

/**
 * Carousel page-dots indicator with infinite cycling and a current-page badge.
 *
 * - Maximum of 5 visible dots that cycle through as the user scrolls
 * - Selected dot highlighted in primary; edge dots shrink to hint at more pages
 * - Optional "1/N" page badge (auto-enabled for >5 items)
 */
@Composable
fun DotsIndicator(
    pageCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    selectedSize: Dp = 8.dp,
    unselectedSize: Dp = 8.dp,
    spacing: Dp = 8.dp,
    showPageNumber: Boolean = false,
    onDotClick: ((Int) -> Unit)? = null,
) {
    if (pageCount <= 1) return

    val shouldShowPageNumber = showPageNumber || pageCount > MAX_VISIBLE_DOTS

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pageCount <= MAX_VISIBLE_DOTS) {
            SimpleDotsRow(pageCount, selectedIndex, selectedSize, unselectedSize, spacing, onDotClick)
        } else {
            CyclingDotsRow(pageCount, selectedIndex, selectedSize, unselectedSize, spacing)
        }

        if (shouldShowPageNumber) {
            Spacer(modifier = Modifier.width(12.dp))
            PageNumberBadge(currentPage = selectedIndex + 1, totalPages = pageCount)
        }
    }
}

@Composable
private fun SimpleDotsRow(
    pageCount: Int,
    selectedIndex: Int,
    selectedSize: Dp,
    unselectedSize: Dp,
    spacing: Dp,
    onDotClick: ((Int) -> Unit)? = null,
) {
    repeat(pageCount) { index ->
        val isSelected = selectedIndex == index

        val animatedSize by animateDpAsState(
            targetValue = if (isSelected) selectedSize else unselectedSize,
            animationSpec = tween(durationMillis = 200),
            label = "dotSize",
        )
        val animatedColor by animateColorAsState(
            targetValue =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            animationSpec = tween(durationMillis = 200),
            label = "dotColor",
        )

        val dotDescription = if (isSelected) "Current page ${index + 1}" else "Page ${index + 1}"
        Box(
            modifier =
                Modifier
                    .size(animatedSize)
                    .background(color = animatedColor, shape = DesignTokens.Shape.button)
                    .semantics { contentDescription = dotDescription }
                    .let { dotModifier ->
                        if (onDotClick != null) {
                            dotModifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onDotClick(index) },
                            )
                        } else {
                            dotModifier
                        }
                    },
        )
        if (index != pageCount - 1) Spacer(modifier = Modifier.width(spacing))
    }
}

@Composable
private fun CyclingDotsRow(
    pageCount: Int,
    selectedIndex: Int,
    selectedSize: Dp,
    unselectedSize: Dp,
    spacing: Dp,
) {
    val windowInfo =
        remember(selectedIndex, pageCount) {
            calculateDotWindow(selectedIndex, pageCount)
        }

    windowInfo.visibleIndices.forEachIndexed { displayIndex, actualIndex ->
        val isSelected = actualIndex == selectedIndex
        val isEdge = windowInfo.isEdgeDot(displayIndex)

        val targetSize =
            when {
                isSelected -> selectedSize
                isEdge -> unselectedSize * 0.7f // Smaller edge dots hint at more pages
                else -> unselectedSize
            }
        val animatedSize by animateDpAsState(
            targetValue = targetSize,
            animationSpec = tween(durationMillis = 200),
            label = "dotSize",
        )

        val targetColor =
            when {
                isSelected -> MaterialTheme.colorScheme.primary
                isEdge -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 200),
            label = "dotColor",
        )

        Box(
            modifier =
                Modifier
                    .size(animatedSize)
                    .background(color = animatedColor, shape = DesignTokens.Shape.button),
        )
        if (displayIndex != windowInfo.visibleIndices.lastIndex) {
            Spacer(modifier = Modifier.width(spacing))
        }
    }
}

private data class DotWindowInfo(
    val visibleIndices: List<Int>,
    val hasLeadingEdge: Boolean,
    val hasTrailingEdge: Boolean,
) {
    fun isEdgeDot(displayIndex: Int): Boolean {
        return (displayIndex == 0 && hasLeadingEdge) ||
            (displayIndex == visibleIndices.lastIndex && hasTrailingEdge)
    }
}

/**
 * Calculates the sliding window of dot indices keeping the selection roughly centred.
 */
private fun calculateDotWindow(
    selectedIndex: Int,
    pageCount: Int,
): DotWindowInfo {
    val visibleCount = MAX_VISIBLE_DOTS
    val halfWindow = visibleCount / 2
    var windowStart = (selectedIndex - halfWindow).coerceAtLeast(0)
    val windowEnd = (windowStart + visibleCount - 1).coerceAtMost(pageCount - 1)
    if (windowEnd - windowStart < visibleCount - 1) {
        windowStart = (windowEnd - visibleCount + 1).coerceAtLeast(0)
    }
    return DotWindowInfo(
        visibleIndices = (windowStart..windowEnd).toList(),
        hasLeadingEdge = windowStart > 0,
        hasTrailingEdge = windowEnd < pageCount - 1,
    )
}

@Composable
private fun PageNumberBadge(
    currentPage: Int,
    totalPages: Int,
) {
    Box(
        modifier =
            Modifier
                .clip(DesignTokens.Shape.button)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$currentPage/$totalPages",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Section title with an optional count badge and contextual icon.
 */
@Composable
fun SectionTitleWithCount(
    title: String,
    count: Int,
    showBadgeThreshold: Int = 2,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.2).sp,
                ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (count > showBadgeThreshold) {
            CountBadge(count = count)
        }
    }
}

/**
 * Small circular/pill count badge.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    textColor: Color = MaterialTheme.colorScheme.primary,
) {
    val displayText = if (count > 99) "99+" else count.toString()
    val shape = DesignTokens.Shape.button

    Box(
        modifier =
            modifier
                .height(20.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(horizontal = if (count > 9) 8.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = textColor,
        )
    }
}

// =============================================================================
// Auto-sizing text
// =============================================================================

/**
 * Text that shrinks its font size (down to [minFontSize]) so important strings, names,
 * greetings, titles, are never ellipsised. Width estimation uses a 0.65 character-width
 * factor, which is safe for wide glyphs.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 180.dp,
    maxFontSize: TextUnit = 22.sp,
    minFontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = 1,
) {
    val density = LocalDensity.current
    val containerWidthPx = LocalWindowInfo.current.containerSize.width
    val screenWidth = with(density) { containerWidthPx.toDp() }

    // On narrow windows scale the budget to a fraction of the window width
    val effectiveMaxWidth =
        remember(screenWidth, maxWidth) {
            val availableWidth =
                when {
                    screenWidth < 360.dp -> screenWidth * 0.6f
                    screenWidth < 400.dp -> screenWidth * 0.7f
                    else -> maxWidth
                }
            availableWidth.coerceAtLeast(200.dp)
        }

    val fontSize =
        remember(text, effectiveMaxWidth, maxFontSize, minFontSize, maxLines) {
            val baseFontSize = maxFontSize.value
            val minSize = minFontSize.value

            val estimatedTextWidthSp = text.length * baseFontSize * 0.65f
            val availableWidthPx = with(density) { effectiveMaxWidth.toPx() }
            val estimatedTextWidthPx = with(density) { estimatedTextWidthSp.sp.toPx() }

            val estimatedLineWidth = estimatedTextWidthPx / maxLines
            if (estimatedLineWidth <= availableWidthPx) {
                maxFontSize
            } else {
                val scaleFactor = availableWidthPx / estimatedLineWidth
                (baseFontSize * scaleFactor).coerceIn(minSize, baseFontSize).sp
            }
        }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        // Clip: the size shrink already guarantees a fit
        overflow = TextOverflow.Clip,
        style = style,
    )
}

/**
 * Convenience greeting + name combination that auto-sizes as one string.
 */
@Composable
fun AutoSizeGreeting(
    greeting: String,
    name: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 180.dp,
    maxFontSize: TextUnit = 22.sp,
    minFontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
) {
    val fullText = if (name.isNotBlank()) "${greeting.trimEnd()} $name" else greeting
    AutoSizeText(
        text = fullText,
        modifier = modifier,
        maxWidth = maxWidth,
        maxFontSize = maxFontSize,
        minFontSize = minFontSize,
        fontWeight = fontWeight,
        color = color,
    )
}

/**
 * Computes the title width budget left over after trailing action icons.
 */
fun calculateTitleMaxWidth(
    screenWidthDp: Int,
    actionIconsCount: Int = 4,
): Dp {
    val actionIconWidth = 48.dp
    val actionSpacing = 8.dp
    val padding = 32.dp

    val totalActionsWidth =
        (actionIconWidth * actionIconsCount) +
            (actionSpacing * (actionIconsCount - 1).coerceAtLeast(0))

    val availableWidth = screenWidthDp.dp - totalActionsWidth - padding

    return availableWidth.coerceIn(120.dp, 200.dp)
}
