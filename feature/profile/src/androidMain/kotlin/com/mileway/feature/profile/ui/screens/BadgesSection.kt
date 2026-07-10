package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.engagement.Badge
import com.mileway.core.data.engagement.BadgeId
import com.mileway.core.data.engagement.Compliment
import com.mileway.core.ui.components.ConfettiBurst
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.badge_first_trip
import com.mileway.core.ui.resources.badge_hundred_km
import com.mileway.core.ui.resources.badge_ten_trips
import com.mileway.core.ui.resources.badge_week_streak
import com.mileway.core.ui.resources.compliment_clean_ride
import com.mileway.core.ui.resources.compliment_great_navigation
import com.mileway.core.ui.resources.compliment_on_time
import com.mileway.core.ui.resources.compliment_safe_driving
import com.mileway.core.ui.resources.profile_badges_compliments
import com.mileway.core.ui.resources.profile_badges_subtitle
import com.mileway.core.ui.resources.profile_badges_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.BadgesViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P12.1: the profile-hub badges + compliments section (Driver-shaped horizontal rows).
 * Earned badges come from real trip milestones ([BadgesViewModel] → BadgeRepository); the rating
 * chip is gated by the `showRating` plugin (hidden when the seeded rating is ≤0). A newly-earned
 * badge fires the reused [ConfettiBurst] overlay. Rendered only when `badgesEnabled` is on (gated by
 * the caller so the disabled hub keeps its golden byte-identical).
 */
@Composable
fun BadgesSection(
    showRating: Boolean,
    modifier: Modifier = Modifier,
    viewModel: BadgesViewModel = koinViewModel(),
) {
    val board by viewModel.state.collectAsStateWithLifecycle()
    val justEarned by viewModel.justEarned.collectAsStateWithLifecycle()

    if (justEarned) {
        LaunchedConfettiConsume(viewModel)
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.Shape.roundedMd,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_badges_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(Res.string.profile_badges_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showRating && board.rating > 0.0) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(oneDecimal(board.rating)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(disabledLabelColor = MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    items(board.badges, key = { it.id.name }) { badge -> BadgeChip(badge) }
                }

                val compliments = board.compliments.filter { it.count > 0 }
                if (compliments.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.profile_badges_compliments),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        items(compliments, key = { it.id }) { c -> ComplimentChip(c) }
                    }
                }
            }
        }

        if (justEarned) {
            ConfettiBurst(modifier = Modifier.matchParentSize(), particleCount = 50, durationMs = 1800)
        }
    }
}

@Composable
private fun LaunchedConfettiConsume(viewModel: BadgesViewModel) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(1800)
        viewModel.consumeConfetti()
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    val accent = if (badge.earned) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(84.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (badge.earned) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = badgeIcon(badge.id),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = badgeLabel(badge.id),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (badge.earned) FontWeight.SemiBold else FontWeight.Normal,
            color = if (badge.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ComplimentChip(compliment: Compliment) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("${complimentLabel(compliment.id)} · ${compliment.count}") },
        leadingIcon = { Icon(complimentIcon(compliment.id), contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(disabledLabelColor = MaterialTheme.colorScheme.onSurface),
    )
}

private fun badgeIcon(id: BadgeId): ImageVector =
    when (id) {
        BadgeId.FIRST_TRIP -> Icons.Filled.EmojiEvents
        BadgeId.TEN_TRIPS -> Icons.Filled.Timeline
        BadgeId.HUNDRED_KM -> Icons.Filled.Route
        BadgeId.WEEK_STREAK -> Icons.Filled.Star
    }

@Composable
private fun badgeLabel(id: BadgeId): String =
    when (id) {
        BadgeId.FIRST_TRIP -> stringResource(Res.string.badge_first_trip)
        BadgeId.TEN_TRIPS -> stringResource(Res.string.badge_ten_trips)
        BadgeId.HUNDRED_KM -> stringResource(Res.string.badge_hundred_km)
        BadgeId.WEEK_STREAK -> stringResource(Res.string.badge_week_streak)
    }

private fun complimentIcon(id: String): ImageVector =
    when (id) {
        "clean_ride" -> Icons.Filled.CleaningServices
        "on_time" -> Icons.Filled.Schedule
        "safe_driving" -> Icons.Filled.Shield
        else -> Icons.Filled.Explore
    }

@Composable
private fun complimentLabel(id: String): String =
    when (id) {
        "clean_ride" -> stringResource(Res.string.compliment_clean_ride)
        "on_time" -> stringResource(Res.string.compliment_on_time)
        "safe_driving" -> stringResource(Res.string.compliment_safe_driving)
        else -> stringResource(Res.string.compliment_great_navigation)
    }

private fun oneDecimal(v: Double): String {
    val scaled = (v * 10).toLong()
    return "${scaled / 10}.${scaled % 10}"
}
