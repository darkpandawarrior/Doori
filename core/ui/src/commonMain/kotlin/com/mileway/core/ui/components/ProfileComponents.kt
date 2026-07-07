package com.mileway.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_almost_there
import com.mileway.core.ui.resources.core_cd_hide_checklist
import com.mileway.core.ui.resources.core_cd_profile_complete
import com.mileway.core.ui.resources.core_cd_show_checklist
import com.mileway.core.ui.resources.core_field_optional
import com.mileway.core.ui.resources.core_field_required
import com.mileway.core.ui.resources.core_profile_all_up_to_date
import com.mileway.core.ui.resources.core_profile_almost_there
import com.mileway.core.ui.resources.core_profile_checklist
import com.mileway.core.ui.resources.core_profile_complete_cta
import com.mileway.core.ui.resources.core_profile_complete_title
import com.mileway.core.ui.resources.core_profile_more_items
import com.mileway.core.ui.resources.core_profile_optional_available
import com.mileway.core.ui.resources.core_profile_required_remaining
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Visual status states for profile grid items.
 * Each status affects the tile's appearance (colors, badges, animations).
 */
enum class ProfileItemStatus {
    /** Item is fully configured and up-to-date */
    COMPLETE,

    /** Item is not set up or has missing information */
    INCOMPLETE,

    /** Item requires user attention or action */
    NEEDS_ATTENTION,

    /** Item is currently being updated (shows loading indicator) */
    UPDATING,
}

/**
 * Data model representing a single item in the profile grid.
 * Used to configure grid tiles with consistent properties and state management.
 *
 * @property id Unique identifier for the item
 * @property title Display title for the tile
 * @property subtitle Descriptive subtitle or status text
 * @property icon Leading icon for visual identification
 * @property category Optional section/category label this item belongs to
 * @property status Visual state of the item (Complete, Incomplete, NeedsAttention, Updating)
 * @property badgeCount Optional numeric badge (e.g., "3 guests", "5 logs")
 * @property isVisible Whether this item should be displayed
 * @property isEnabled Whether the item is clickable
 * @property priority Display priority for smart ordering (higher = shown first)
 * @property lastUpdated Timestamp of last update for "Recently Updated" sorting
 * @property customContainerColor Optional override for tile background color
 * @property customContentColor Optional override for tile icon/text color
 * @property gradientColors Optional list of colors for a gradient background
 * @property animatePulse Whether to show a subtle pulse animation
 * @property animateShimmer Whether to show a shimmer sweep animation
 * @property action Callback when the tile is clicked
 */
data class ProfileGridItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: String? = null,
    val status: ProfileItemStatus = ProfileItemStatus.COMPLETE,
    val badgeCount: Int? = null,
    val isVisible: Boolean = true,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val lastUpdated: Long? = null,
    val customContainerColor: Color? = null,
    val customContentColor: Color? = null,
    val gradientColors: List<Color>? = null,
    val animatePulse: Boolean = false,
    val animateShimmer: Boolean = false,
    val action: () -> Unit,
)

/**
 * A square grid tile for displaying profile information items.
 * Features icon at top, title, subtitle, and visual state indicators.
 *
 * @param item The profile grid item containing all configuration
 * @param modifier Optional modifier for the tile
 * @param compact Whether to use a slightly wider, denser layout
 */
@Composable
fun GridProfileTile(
    item: ProfileGridItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current

    // Pulse animation for needs attention state
    val infinite = rememberInfiniteTransition(label = "tile_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_scale",
    )

    // Shimmer animation for updating state or custom shimmer
    val shimmer by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_offset",
    )

    // Determine colors based on status
    val (baseContainerColor, baseContentColor, baseBorderColor) =
        when (item.status) {
            ProfileItemStatus.COMPLETE ->
                Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
            ProfileItemStatus.INCOMPLETE ->
                Triple(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
            ProfileItemStatus.NEEDS_ATTENTION ->
                Triple(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.onErrorContainer,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                )
            ProfileItemStatus.UPDATING ->
                Triple(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                )
        }

    val containerColor = item.customContainerColor ?: baseContainerColor
    val contentColor = item.customContentColor ?: baseContentColor
    val borderColor =
        if (item.customContainerColor != null || item.gradientColors != null) {
            contentColor.copy(alpha = 0.3f)
        } else {
            baseBorderColor
        }

    // Gradient background
    val backgroundBrush =
        item.gradientColors?.let { colors ->
            if (item.animateShimmer) {
                Brush.horizontalGradient(
                    colors = colors,
                    startX = shimmer * 1000f,
                    endX = (shimmer + 1f) * 1000f,
                )
            } else {
                Brush.linearGradient(colors)
            }
        }

    Card(
        modifier =
            modifier
                .aspectRatio(if (compact) 1.18f else 1f)
                .scale(if (item.status == ProfileItemStatus.NEEDS_ATTENTION || item.animatePulse) pulse else 1f)
                .clickable(enabled = item.isEnabled && item.status != ProfileItemStatus.UPDATING) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    item.action()
                }
                .border(
                    width = if (item.status == ProfileItemStatus.INCOMPLETE) 2.dp else 1.dp,
                    color = borderColor,
                    shape = DesignTokens.Shape.roundedMd,
                ),
        shape = DesignTokens.Shape.roundedMd,
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBrush != null) {
                            Modifier.background(backgroundBrush)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            // Shimmer effect for updating state (standard overlay)
            if (item.status == ProfileItemStatus.UPDATING && item.gradientColors == null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                Color.Transparent,
                                            ),
                                        startX = shimmer * 500f,
                                        endX = (shimmer + 1f) * 500f,
                                    ),
                            ),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(if (compact) 10.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Icon with background and badge
                val iconBaseColor =
                    if (item.customContentColor != null) {
                        item.customContentColor
                    } else if (item.status == ProfileItemStatus.COMPLETE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        contentColor
                    }

                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Circular background for icon
                    Box(
                        modifier =
                            Modifier
                                .size(if (compact) 36.dp else 40.dp)
                                .background(
                                    color = iconBaseColor.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                            tint = iconBaseColor,
                        )
                    }

                    // Badge count - positioned at top-right with offset to keep icon visible
                    item.badgeCount?.let { count ->
                        if (count > 0) {
                            // Calculate offset based on badge width (larger counts need more offset)
                            val horizontalOffset =
                                when {
                                    count > 99 -> 10.dp // "99+" badge is wider
                                    count > 9 -> 8.dp // Double digit
                                    else -> 7.dp // Single digit
                                }

                            Badge(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = horizontalOffset, y = (-6).dp),
                            ) {
                                Text(
                                    text = if (count > 99) "99+" else count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    // Status indicator dot
                    when (item.status) {
                        ProfileItemStatus.NEEDS_ATTENTION -> {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .border(1.5.dp, containerColor, CircleShape),
                            )
                        }
                        ProfileItemStatus.UPDATING -> {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp),
                                strokeWidth = 2.dp,
                                color = iconBaseColor,
                            )
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))

                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                )

                Spacer(modifier = Modifier.height(if (compact) 2.dp else 3.dp))

                // Subtitle
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * A section header for grouping profile items into logical categories.
 * Displays an optional contextual icon, the category name, and an optional count badge.
 *
 * @param title The section title/category name
 * @param itemCount Optional count of items in this section
 * @param icon Optional contextual icon to display before the title
 * @param modifier Optional modifier for the header
 */
@Composable
fun ProfileSectionHeader(
    title: String,
    itemCount: Int? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.s, horizontal = DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        // Optional contextual icon
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSize.navigation),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Optional item count badge
        itemCount?.let { count ->
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier =
                        Modifier.padding(
                            horizontal = DesignTokens.Spacing.s,
                            vertical = DesignTokens.Spacing.xs,
                        ),
                )
            }
        }
    }
}

/**
 * Data class representing a missing/incomplete item for display in the banner.
 */
data class MissingItemDisplay(
    val id: String,
    val title: String,
    val isRequired: Boolean,
)

/**
 * Data class representing completion statistics for a category.
 */
data class CategoryCompletionDisplay(
    val categoryLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val percentage: Int,
    val isRequiredCategory: Boolean,
)

/**
 * Enhanced banner displaying profile completion progress with gamification elements.
 * Shows percentage complete, X/Y counter, missing items, category pills, and expandable checklist.
 *
 * @param completionPercentage Profile completion percentage (0-100)
 * @param completedCount Number of completed items (X in "X of Y")
 * @param totalCount Total number of items (Y in "X of Y")
 * @param missingItems List of incomplete items with metadata
 * @param categories List of category completion statistics
 * @param expanded Whether the checklist is currently expanded
 * @param onExpandToggle Callback when user taps to expand/collapse checklist
 * @param onCompleteClick Callback when user clicks to complete missing items
 * @param onMissingItemClick Callback when user clicks a specific missing item
 * @param modifier Optional modifier for the banner
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCompletionBanner(
    completionPercentage: Int,
    completedCount: Int,
    totalCount: Int,
    missingItems: List<MissingItemDisplay>,
    categories: List<CategoryCompletionDisplay>,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    onMissingItemClick: ((String) -> Unit)? = null,
) {
    // Pulsing animation for incomplete state
    val infinite = rememberInfiniteTransition(label = "completion_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_scale",
    )

    // Glow effect for progress ring
    val glow by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glow_alpha",
    )

    // Progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress_animation",
    )

    val isComplete = completionPercentage >= 100
    val isHighProgress = completionPercentage >= 75
    val hasMissingItems = missingItems.isNotEmpty()

    // Dynamic colors based on progress
    val progressColor =
        when {
            isComplete -> MaterialTheme.colorScheme.primary
            isHighProgress -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickable(enabled = hasMissingItems && !expanded) { onCompleteClick() },
        shape = DesignTokens.Shape.roundedLg,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isComplete) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Gradient background accent
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        if (isComplete) {
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            )
                                        } else {
                                            listOf(
                                                progressColor.copy(alpha = 0.7f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                            )
                                        },
                                ),
                        ),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.l),
            ) {
                // Main banner row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                ) {
                    // Circular progress indicator with glow effect
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(68.dp)
                                .scale(if (!isComplete && isHighProgress) pulse else 1f),
                    ) {
                        // Glow background layer
                        if (!isComplete) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .scale(1.1f),
                                color = progressColor.copy(alpha = glow * 0.3f),
                                strokeWidth = 8.dp,
                                trackColor = Color.Transparent,
                            )
                        }

                        // Main progress indicator
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = progressColor,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        )

                        if (isComplete) {
                            // Success icon with scale animation
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(Res.string.core_cd_profile_complete),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .scale(pulse),
                            )
                        } else {
                            // Percentage text
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "$completionPercentage%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                if (isHighProgress) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = stringResource(Res.string.core_cd_almost_there),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Text content
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                    ) {
                        Text(
                            text =
                                when {
                                    isComplete -> stringResource(Res.string.core_profile_complete_title)
                                    isHighProgress -> stringResource(Res.string.core_profile_almost_there)
                                    else -> stringResource(Res.string.core_profile_complete_cta)
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (isComplete) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                        )

                        // X of Y complete counter
                        Text(
                            text = "$completedCount of $totalCount complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (isComplete) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                },
                        )

                        // Missing items summary (only when not expanded)
                        if (!isComplete && !expanded && hasMissingItems) {
                            val requiredMissing = missingItems.filter { it.isRequired }
                            val text =
                                if (requiredMissing.isNotEmpty()) {
                                    stringResource(Res.string.core_profile_required_remaining, requiredMissing.size)
                                } else {
                                    stringResource(Res.string.core_profile_optional_available, missingItems.size)
                                }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (isComplete) {
                            Text(
                                text = stringResource(Res.string.core_profile_all_up_to_date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }

                    // Expand/Collapse or Action indicator
                    if (hasMissingItems) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHighProgress) {
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        },
                                    )
                                    .clickable { onExpandToggle() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription =
                                    if (expanded) {
                                        stringResource(
                                            Res.string.core_cd_hide_checklist,
                                        )
                                    } else {
                                        stringResource(Res.string.core_cd_show_checklist)
                                    },
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(DesignTokens.IconSize.header),
                            )
                        }
                    }
                }

                // Category pills row
                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.m))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        categories.forEach { category ->
                            CategoryPill(
                                category = category,
                                isComplete = isComplete,
                            )
                        }
                    }
                }

                // Expandable checklist section
                if (expanded && hasMissingItems) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.l))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedSm,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.m),
                        ) {
                            Text(
                                text = stringResource(Res.string.core_profile_checklist),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.s))

                            // Show required items first
                            val requiredItems = missingItems.filter { it.isRequired }
                            val optionalItems = missingItems.filter { !it.isRequired }

                            if (requiredItems.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.core_field_required),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                                requiredItems.forEach { item ->
                                    MissingItemRow(
                                        item = item,
                                        onClick = { onMissingItemClick?.invoke(item.id) },
                                    )
                                }
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.s))
                            }

                            if (optionalItems.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.core_field_optional),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                                optionalItems.take(5).forEach { item ->
                                    MissingItemRow(
                                        item = item,
                                        onClick = { onMissingItemClick?.invoke(item.id) },
                                    )
                                }
                                if (optionalItems.size > 5) {
                                    Text(
                                        text = stringResource(Res.string.core_profile_more_items, optionalItems.size - 5),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category pill showing completion status for a specific category.
 */
@Composable
private fun CategoryPill(
    category: CategoryCompletionDisplay,
    isComplete: Boolean,
) {
    val backgroundColor =
        when {
            isComplete -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            category.percentage >= 75 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            category.percentage >= 50 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }

    val textColor =
        when {
            isComplete -> MaterialTheme.colorScheme.primary
            category.percentage >= 75 -> MaterialTheme.colorScheme.tertiary
            category.percentage >= 50 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        shape = DesignTokens.Shape.roundedMd,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Text(
                text = "${category.completedCount}/${category.totalCount}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Text(
                text = category.categoryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.9f),
            )
        }
    }
}

/**
 * Row showing a single missing item in the checklist.
 */
@Composable
private fun MissingItemRow(
    item: MissingItemDisplay,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        // Status indicator
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isRequired) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint =
                    if (item.isRequired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(14.dp),
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (item.isRequired) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            fontWeight = if (item.isRequired) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )

        if (item.isRequired) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            ) {
                Text(
                    text = stringResource(Res.string.core_field_required),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
