package com.mileway.feature.logging.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.ui.components.sheet.SearchablePickerSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_choose_vehicle_type
import com.mileway.core.ui.resources.logging_search_vehicles_placeholder
import com.mileway.core.ui.resources.logging_vehicle_fallback
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/** Picks an icon for a vehicle from its display name (two-wheeler vs taxi vs car). */
private fun vehicleIcon(name: String?): ImageVector {
    val n = name?.lowercase().orEmpty()
    return when {
        "two" in n || "bike" in n || "cycle" in n || "scooter" in n -> Icons.Filled.DirectionsBike
        "taxi" in n || "cab" in n || "meter" in n -> Icons.Filled.LocalTaxi
        else -> Icons.Filled.DirectionsCar
    }
}

/**
 * Modal "Choose Vehicle Type" sheet: a search field over the approved vehicles
 * plus a two-column grid of icon tiles. Selecting a tile confirms it via
 * [onSelect] and dismisses the sheet.
 *
 * @param vehicles   approved vehicles from the ViewModel
 * @param onSelect   called with the chosen vehicle
 * @param onDismiss  called when the sheet is dismissed without a selection
 */
@Composable
fun VehiclePickerSheet(
    vehicles: List<ApprovedVehicle>,
    onSelect: (ApprovedVehicle) -> Unit,
    onDismiss: () -> Unit,
) {
    // SHEETS.C: the modal + title + search field + filtering come from the shared SearchablePickerSheet;
    // only the vehicle-specific 2-column tile grid is supplied via the results slot.
    SearchablePickerSheet(
        title = stringResource(Res.string.logging_choose_vehicle_type),
        items = vehicles,
        filter = { vehicle, query -> vehicle.vehicleName?.contains(query, ignoreCase = true) == true },
        searchPlaceholder = stringResource(Res.string.logging_search_vehicles_placeholder),
        onDismiss = onDismiss,
    ) { filtered, _ ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            items(filtered, key = { it.vehicleKey ?: it.hashCode().toString() }) { vehicle ->
                VehicleTile(vehicle = vehicle, onClick = { onSelect(vehicle) })
            }
        }
    }
}

@Composable
private fun VehicleTile(
    vehicle: ApprovedVehicle,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = vehicleIcon(vehicle.vehicleName),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(DesignTokens.IconSize.header),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                text = vehicle.vehicleName ?: stringResource(Res.string.logging_vehicle_fallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            vehicle.vehiclePricing?.let { rate ->
                Text(
                    text = "₹${rate.formatDecimal(1)}/km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
