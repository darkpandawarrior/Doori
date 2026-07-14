@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.logging.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.siddharth.kmp.common.formatDecimal
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_add_location
import com.mileway.core.ui.resources.logging_add_stop_here
import com.mileway.core.ui.resources.logging_amount
import com.mileway.core.ui.resources.logging_current
import com.mileway.core.ui.resources.logging_edit
import com.mileway.core.ui.resources.logging_edit_locations_cd
import com.mileway.core.ui.resources.logging_empty_itinerary
import com.mileway.core.ui.resources.logging_move_down_cd
import com.mileway.core.ui.resources.logging_move_up_cd
import com.mileway.core.ui.resources.logging_pricing
import com.mileway.core.ui.resources.logging_remove
import com.mileway.core.ui.resources.logging_reorder_tip
import com.mileway.core.ui.resources.logging_return_to_start
import com.mileway.core.ui.resources.logging_round_trip
import com.mileway.core.ui.resources.logging_to_cd
import com.mileway.core.ui.resources.logging_total_distance
import com.mileway.core.ui.resources.logging_travelled_locations
import com.mileway.core.ui.resources.logging_travelled_locations_subtitle
import com.mileway.core.ui.resources.logging_verify
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.ui.model.LocationStop
import org.jetbrains.compose.resources.stringResource

/**
 * Callbacks for editing the travelled-locations itinerary. Bundled so both the
 * Step 1 and Step 2 cards can share one parameter surface.
 */
data class TravelledLocationsActions(
    val onEdit: (stopId: Long) -> Unit,
    val onRemove: (stopId: Long) -> Unit,
    val onMoveUp: (index: Int) -> Unit,
    val onMoveDown: (index: Int) -> Unit,
    val onInsertAfter: (index: Int) -> Unit,
    val onToggleRoundTrip: (Boolean) -> Unit,
    val onAddLocation: () -> Unit,
    val onUseCurrent: () -> Unit,
    val onVerifyDistance: () -> Unit,
)

/**
 * The "Travelled locations" card from the reference. Shows the live total
 * distance, a round-trip toggle, pricing/amount, and the numbered, reorderable
 * stop list with per-stop Edit/Remove and "Add stop here" insertion affordances.
 *
 * Reorder uses up/down arrow buttons (a pragmatic stand-in for real drag) so the
 * card stays usable without a drag framework. When [compact] the distance hero
 * and pricing read smaller, used on the Step 2 recap.
 *
 * @param stops          ordered itinerary stops
 * @param totalDistanceKm distance currently applied (calculated or overridden)
 * @param pricePerKm     selected vehicle's per-km rate
 * @param amount         computed reimbursable amount
 * @param isRoundTrip    round-trip state
 * @param actions        edit callbacks
 * @param compact        denser layout for the Step 2 recap
 */
@Composable
fun TravelledLocationsCard(
    stops: List<LocationStop>,
    totalDistanceKm: Double,
    pricePerKm: Double,
    amount: Double,
    isRoundTrip: Boolean,
    actions: TravelledLocationsActions,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            // Header with edit pencil.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.logging_travelled_locations),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(Res.string.logging_travelled_locations_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(Res.string.logging_edit_locations_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Distance hero + round-trip toggle.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.logging_total_distance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${totalDistanceKm.formatDecimal(2)} km",
                            style =
                                if (compact) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.displaySmall
                                },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (stops.size >= 2) {
                            Spacer(Modifier.size(DesignTokens.Spacing.s))
                            TextButton(
                                onClick = actions.onVerifyDistance,
                                shape = DesignTokens.Shape.button,
                            ) { Text(stringResource(Res.string.logging_verify)) }
                        }
                    }
                }
                Surface(
                    shape = DesignTokens.Shape.roundedSm,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = DesignTokens.Spacing.m,
                                vertical = DesignTokens.Spacing.s,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(Res.string.logging_round_trip), style = MaterialTheme.typography.labelLarge)
                            Text(
                                stringResource(Res.string.logging_return_to_start),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(DesignTokens.Spacing.s))
                        Switch(checked = isRoundTrip, onCheckedChange = actions.onToggleRoundTrip)
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Pricing + amount.
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.logging_pricing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "₹${pricePerKm.formatDecimal(1)}/km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.logging_amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "₹${amount.formatDecimal(2)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            if (stops.isEmpty()) {
                Text(
                    stringResource(Res.string.logging_empty_itinerary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = DesignTokens.Spacing.s),
                )
            } else {
                Text(
                    stringResource(Res.string.logging_reorder_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                stops.forEachIndexed { index, stop ->
                    StopRow(
                        index = index,
                        stop = stop,
                        isFirst = index == 0,
                        isLast = index == stops.lastIndex,
                        actions = actions,
                    )
                    // "Add stop here" insertion affordance between stops.
                    if (index < stops.lastIndex) {
                        InsertAffordance(onClick = { actions.onInsertAfter(index) })
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Add location + Current buttons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(
                    onClick = actions.onAddLocation,
                    modifier = Modifier.weight(1f),
                    shape = DesignTokens.Shape.button,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
                    Spacer(Modifier.size(DesignTokens.Spacing.xs))
                    Text(stringResource(Res.string.logging_add_location))
                }
                OutlinedButton(
                    onClick = actions.onUseCurrent,
                    modifier = Modifier.weight(1f),
                    shape = DesignTokens.Shape.button,
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
                    Spacer(Modifier.size(DesignTokens.Spacing.xs))
                    Text(stringResource(Res.string.logging_current))
                }
            }
        }
    }
}

@Composable
private fun StopRow(
    index: Int,
    stop: LocationStop,
    isFirst: Boolean,
    isLast: Boolean,
    actions: TravelledLocationsActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Numbered index badge.
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.size(DesignTokens.Spacing.m))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stop.entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                EditableChip(
                    label = stringResource(Res.string.logging_edit),
                    icon = Icons.Filled.Edit,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { actions.onEdit(stop.id) },
                )
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                EditableChip(
                    label = stringResource(Res.string.logging_remove),
                    icon = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { actions.onRemove(stop.id) },
                )
            }
        }

        // Up/down reorder controls (drag stand-in).
        Column {
            IconButton(
                onClick = { actions.onMoveUp(index) },
                enabled = !isFirst,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(Res.string.logging_move_up_cd),
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
            }
            IconButton(
                onClick = { actions.onMoveDown(index) },
                enabled = !isLast,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(Res.string.logging_move_down_cd),
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
            }
        }
    }
}

@Composable
private fun EditableChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = tint.copy(alpha = 0.10f),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@Composable
private fun InsertAffordance(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClick, shape = DesignTokens.Shape.button) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Text(stringResource(Res.string.logging_add_stop_here), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Tiny legend chip used by the Step 2 checklist header (Locations/Vehicle/...). */
@Composable
fun ChecklistChip(
    label: String,
    satisfied: Boolean,
    modifier: Modifier = Modifier,
) {
    val container =
        if (satisfied) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
    val content =
        if (satisfied) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(modifier = modifier, shape = DesignTokens.Shape.chip, color = container) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.s),
        )
    }
}

/** Compact origin→destination summary row reused on Step 2's recap header. */
@Composable
fun RouteSummaryRow(
    originName: String,
    destinationName: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            originName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(Res.string.logging_to_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DesignTokens.IconSize.inline),
        )
        Text(
            destinationName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
