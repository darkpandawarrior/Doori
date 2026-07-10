package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_dest_disable
import com.mileway.core.ui.resources.tracking_dest_heading_to
import com.mileway.core.ui.resources.tracking_dest_no_places
import com.mileway.core.ui.resources.tracking_dest_pick
import com.mileway.core.ui.resources.tracking_dest_region_central
import com.mileway.core.ui.resources.tracking_dest_region_east
import com.mileway.core.ui.resources.tracking_dest_region_north
import com.mileway.core.ui.resources.tracking_dest_region_south
import com.mileway.core.ui.resources.tracking_dest_region_west
import com.mileway.core.ui.resources.tracking_dest_regions
import com.mileway.core.ui.resources.tracking_dest_remaining
import com.mileway.core.ui.resources.tracking_dest_subtitle
import com.mileway.core.ui.resources.tracking_dest_tagged
import com.mileway.core.ui.resources.tracking_dest_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.tracking.viewmodel.DestinationModeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P11.3 — the "Head home" panel shown on the tracking screen when the `destinationMode`
 * plugin is on. Pick a saved place → active state shows the destination + a live countdown → Disable
 * (or auto-expire). Trips started while active are auto-classified toward the destination (see
 * [com.mileway.feature.tracking.viewmodel.TrackMilesViewModel]). The region chips are a preference
 * store only — selecting them persists a set, with no routing behaviour (ponytail ceiling).
 */
@Composable
fun DestinationModePanel(
    modifier: Modifier = Modifier,
    viewModel: DestinationModeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = stringResource(Res.string.tracking_dest_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.tracking_dest_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.active) {
                Text(
                    text = stringResource(Res.string.tracking_dest_heading_to, state.address),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.tracking_dest_remaining, formatCountdown(state.remainingMs)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(
                        onClick = { viewModel.disable() },
                        shape = DesignTokens.Shape.button,
                    ) {
                        Text(stringResource(Res.string.tracking_dest_disable))
                    }
                }
            } else if (state.places.isEmpty()) {
                Text(
                    text = stringResource(Res.string.tracking_dest_no_places),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(Res.string.tracking_dest_pick),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.places.take(4).forEach { place ->
                    OutlinedButton(
                        onClick = { viewModel.pickDestination(place) },
                        shape = DesignTokens.Shape.button,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "  ${place.label}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (state.taggedTrips > 0) {
                Text(
                    text = stringResource(Res.string.tracking_dest_tagged, state.taggedTrips),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(Res.string.tracking_dest_regions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                state.regions.forEach { region ->
                    FilterChip(
                        selected = region.selected,
                        onClick = { viewModel.toggleRegion(region.id) },
                        label = { Text(regionName(region.id, region.name)) },
                        shape = DesignTokens.Shape.chip,
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }
    }
}

/** Localized region label; falls back to the seeded English name for an unknown id. */
@Composable
private fun regionName(
    id: String,
    fallback: String,
): String =
    when (id) {
        "north" -> stringResource(Res.string.tracking_dest_region_north)
        "south" -> stringResource(Res.string.tracking_dest_region_south)
        "east" -> stringResource(Res.string.tracking_dest_region_east)
        "west" -> stringResource(Res.string.tracking_dest_region_west)
        "central" -> stringResource(Res.string.tracking_dest_region_central)
        else -> fallback
    }

/** mm:ss for the head-home countdown. */
private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
