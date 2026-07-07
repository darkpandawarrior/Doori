package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_open_in_maps
import com.mileway.core.ui.resources.tracking_vehicle_picker_title
import com.mileway.core.ui.resources.tracking_vehicle_search_placeholder
import com.mileway.core.ui.resources.tracking_vendor_not_defined
import com.mileway.core.ui.resources.tracking_vendor_picker_title
import com.mileway.core.ui.resources.tracking_vendor_search_placeholder
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/** A selectable vehicle with its per-km reimbursement rate. */
data class VehicleOption(
    val key: String,
    val name: String,
    val ratePerKm: Double,
    val icon: ImageVector = Icons.Filled.DirectionsCar,
)

/**
 * "Choose Vehicle Type", a search field over a 2-column grid of vehicle cards
 * (icon, name, "₹X.X/km"). Stateless: query + selection are hoisted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclePickerSheet(
    vehicles: List<VehicleOption>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val filtered = vehicles.filter { it.name.contains(query, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.tracking_vehicle_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.m),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.tracking_vehicle_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = DesignTokens.Shape.roundedSm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                modifier = Modifier.heightIn(max = 460.dp),
            ) {
                items(filtered, key = { it.key }) { v ->
                    VehicleCard(v, onClick = { onSelect(v.key) })
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleOption,
    onClick: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = vehicle.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${vehicle.ratePerKm.formatDecimal(1)}/km",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A vendor/center the journey can be checked in against. */
data class CenterOption(
    val id: String,
    val name: String,
    val address: String?,
)

/**
 * "List of Centers", a search field over a list of vendor rows (name, address or
 * "Not Defined", open-in-maps trailing action). Stateless: query + selection are hoisted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPickerSheet(
    centers: List<CenterOption>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onOpenMaps: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val filtered =
        centers.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.address?.contains(query, ignoreCase = true) == true)
        }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.tracking_vendor_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.m),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.tracking_vendor_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = DesignTokens.Shape.roundedSm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                modifier = Modifier.heightIn(max = 460.dp),
            ) {
                items(filtered, key = { it.id }) { c ->
                    CenterRow(c, onClick = { onSelect(c.id) }, onOpenMaps = { onOpenMaps(c.id) })
                }
            }
        }
    }
}

@Composable
private fun CenterRow(
    center: CenterOption,
    onClick: () -> Unit,
    onOpenMaps: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = center.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = center.address?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.tracking_vendor_not_defined),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenMaps) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(Res.string.tracking_cd_open_in_maps),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
