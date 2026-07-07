package com.mileway.core.ui.components.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.TrackMilesMainActionButton
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_fab_actions
import com.mileway.core.ui.resources.core_geo_check_in
import com.mileway.core.ui.resources.core_quick_actions
import com.mileway.core.ui.resources.core_track_start
import com.mileway.core.ui.resources.core_track_stop
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * A single quick action surfaced in the expanding grid above the control cluster.
 *
 * @property destructive renders the tile in the error color (e.g. Discard).
 * @property tint optional explicit container color; defaults to a rotating accent palette.
 */
data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color? = null,
    val destructive: Boolean = false,
)

/**
 * The signature bottom control cluster for the tracking screen.
 *
 * - A large hero START/STOP button (primary when idle, red when active).
 * - When active: a Pause/Resume FAB and a Quick-Actions FAB flank the hero.
 * - Tapping Quick-Actions expands a color-coded action grid over a scrim.
 * - An optional bright-green geo check-in chip floats above the hero.
 *
 * Fully stateless except the internal menu-expanded flag. All intents leave via callbacks.
 */
@Composable
fun ThreeButtonFabSystem(
    isActive: Boolean,
    isPaused: Boolean,
    actions: List<QuickAction>,
    onHero: () -> Unit,
    onPauseResume: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    showGeoCheckIn: Boolean = false,
    onGeoCheckIn: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Scrim + quick-action grid (drawn above everything when expanded).
        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { menuExpanded = false }
                    .padding(DesignTokens.Spacing.l),
            ) {
                QuickActionGrid(
                    actions = actions,
                    onAction = { id ->
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        menuExpanded = false
                        onAction(id)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Bright-green geo check-in chip.
            AnimatedVisibility(visible = showGeoCheckIn && isActive) {
                GeoCheckInChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onGeoCheckIn()
                    },
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isActive) {
                    SideFab(
                        icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        label = if (isPaused) "Resume" else "Pause",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onPauseResume()
                        },
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.xl))
                }

                // Hero START / STOP.
                TrackMilesMainActionButton(
                    text = if (isActive) stringResource(Res.string.core_track_stop) else stringResource(Res.string.core_track_start),
                    icon = if (isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHero()
                    },
                    containerColor =
                        if (isActive) {
                            DesignTokens.StatusColors.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    contentColor = Color.White,
                )

                if (isActive) {
                    Spacer(Modifier.width(DesignTokens.Spacing.xl))
                    SideFab(
                        icon = if (menuExpanded) Icons.Filled.Close else Icons.Filled.Apps,
                        label = stringResource(Res.string.core_fab_actions),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            menuExpanded = !menuExpanded
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SideFab(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier =
                Modifier
                    .size(56.dp)
                    .shadow(6.dp, DesignTokens.Shape.button),
            shape = DesignTokens.Shape.button,
            color = container,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GeoCheckInChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.Shape.chip,
        color = DesignTokens.StatusColors.success,
        onClick = onClick,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.core_geo_check_in),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/**
 * The expanding 3-column grid of color-coded quick actions. A staggered scale-in keeps the
 * reveal lively. Each tile is a rounded colored card with an icon over a label.
 */
@Composable
private fun QuickActionGrid(
    actions: List<QuickAction>,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A rotating accent palette so tiles read as distinct categories (matching the source grid).
    val palette =
        listOf(
            DesignTokens.StatusColors.info,
            DesignTokens.StatusColors.success,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            DesignTokens.StatusColors.warning,
            MaterialTheme.colorScheme.secondary,
        )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(DesignTokens.Spacing.l)) {
            Text(
                text = stringResource(Res.string.core_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            actions.chunked(3).forEach { rowActions ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = DesignTokens.Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    rowActions.forEachIndexed { i, action ->
                        val tint =
                            when {
                                action.destructive -> DesignTokens.StatusColors.error
                                action.tint != null -> action.tint
                                else -> palette[(actions.indexOf(action)) % palette.size]
                            }
                        QuickActionTile(action, tint, onAction, Modifier.weight(1f))
                    }
                    repeat(3 - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    tint: Color,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = DesignTokens.Shape.button,
        color = tint,
        onClick = { onAction(action.id) },
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.s),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(DesignTokens.Shape.button)
                        .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
